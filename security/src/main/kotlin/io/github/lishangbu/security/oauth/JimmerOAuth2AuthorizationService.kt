package io.github.lishangbu.security.oauth

import io.github.lishangbu.security.entity.OAuth2AuthorizationRecord
import io.github.lishangbu.security.entity.accessTokenValue
import io.github.lishangbu.security.entity.authorizationCodeValue
import io.github.lishangbu.security.entity.deviceCodeValue
import io.github.lishangbu.security.entity.oidcIdTokenValue
import io.github.lishangbu.security.entity.refreshTokenValue
import io.github.lishangbu.security.entity.state
import io.github.lishangbu.security.entity.userCodeValue
import io.github.lishangbu.security.repository.OAuth2AuthorizationRecordRepository
import io.github.lishangbu.security.entity.OAuthRefreshTokenReplay
import io.github.lishangbu.security.repository.OAuthRefreshTokenReplayRepository
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.or
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2DeviceCode
import org.springframework.security.oauth2.core.OAuth2RefreshToken
import org.springframework.security.oauth2.core.OAuth2Token
import org.springframework.security.oauth2.core.OAuth2UserCode
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import java.time.Instant
import java.time.OffsetDateTime
import java.security.Principal
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import java.time.ZoneOffset
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper
import java.security.MessageDigest
import java.util.HexFormat
import org.springframework.transaction.annotation.Transactional
import org.babyfish.jimmer.sql.ast.mutation.SaveMode

/**
 * 基于 Jimmer 的 Spring Authorization Server 授权记录服务。
 *
 * SAS 标准表仍作为授权记录事实来源，Repository 负责主键保存和删除，按 token
 * 查找使用 Jimmer 类型安全查询，避免运行时依赖 Spring JDBC 或 SAS JDBC 服务。
 */
open class JimmerOAuth2AuthorizationService(
	private val authorizationRepository: OAuth2AuthorizationRecordRepository,
	private val registeredClientRepository: RegisteredClientRepository,
	private val sqlClient: KSqlClient,
	private val objectMapper: ObjectMapper,
	private val refreshTokenReplays: OAuthRefreshTokenReplayRepository,
) : OAuth2AuthorizationService {
	private val mapType = object : TypeReference<Map<String, Any>>() {}

	@Transactional
	open override fun save(authorization: OAuth2Authorization) {
		val previousRefresh = authorizationRepository.findNullable(authorization.id)?.refreshTokenValue
		val nextRefresh = authorization.refreshToken?.token?.tokenValue
		authorizationRepository.save(authorization.toRecord())
		if (previousRefresh != null && previousRefresh != nextRefresh) {
			refreshTokenReplays.save(OAuthRefreshTokenReplay {
				tokenHash = previousRefresh.sha256()
				authorizationId = authorization.id
				recordedAt = Instant.now()
			}, SaveMode.INSERT_ONLY)
		}
	}

	override fun remove(authorization: OAuth2Authorization) {
		authorizationRepository.deleteById(authorization.id)
	}

	override fun findById(id: String): OAuth2Authorization? =
		authorizationRepository.findNullable(id)?.toAuthorization()

	@Transactional
	open override fun findByToken(token: String, tokenType: OAuth2TokenType?): OAuth2Authorization? {
		findRecordByToken(token, tokenType)?.let { return it.toAuthorization() }
		if (tokenType?.value != OAuth2TokenType.REFRESH_TOKEN.value) return null
		val replay = refreshTokenReplays.findNullable(token.sha256()) ?: return null
		// 旧 refresh token 重放意味着 family 可能泄漏，删除当前 authorization 使旋转后的令牌同时失效。
		throw RefreshTokenReplayDetectedException(replay.authorizationId)
	}

	private fun findRecordByToken(token: String, tokenType: OAuth2TokenType?): OAuth2AuthorizationRecord? {
		val tokenTypeValue = tokenType?.value
		if (tokenTypeValue != null && tokenTypeValue !in TOKEN_TYPE_VALUES) {
			return null
		}
		val query = sqlClient.createQuery(OAuth2AuthorizationRecord::class) {
			when (tokenTypeValue) {
				null -> where(
					or(
						table.state eq token,
						table.authorizationCodeValue eq token,
						table.accessTokenValue eq token,
						table.oidcIdTokenValue eq token,
						table.refreshTokenValue eq token,
						table.userCodeValue eq token,
						table.deviceCodeValue eq token,
					),
				)
				STATE_TOKEN_TYPE -> where(table.state eq token)
				CODE_TOKEN_TYPE, AUTHORIZATION_CODE_TOKEN_TYPE -> where(table.authorizationCodeValue eq token)
				OAuth2TokenType.ACCESS_TOKEN.value -> where(table.accessTokenValue eq token)
				ID_TOKEN_TYPE -> where(table.oidcIdTokenValue eq token)
				OAuth2TokenType.REFRESH_TOKEN.value -> where(table.refreshTokenValue eq token)
				USER_CODE_TOKEN_TYPE -> where(table.userCodeValue eq token)
				DEVICE_CODE_TOKEN_TYPE -> where(table.deviceCodeValue eq token)
			}
			select(table)
		}
		return (if (tokenTypeValue == OAuth2TokenType.REFRESH_TOKEN.value) query.forUpdate() else query)
			.limit(1).execute().firstOrNull()
	}

	private fun OAuth2Authorization.toRecord(): OAuth2AuthorizationRecord {
		val authorizationCode = getToken(OAuth2AuthorizationCode::class.java)
		val accessToken = accessToken
		val oidcIdToken = getToken(OidcIdToken::class.java)
		val refreshToken = refreshToken
		val userCode = getToken(OAuth2UserCode::class.java)
		val deviceCode = getToken(OAuth2DeviceCode::class.java)
		return OAuth2AuthorizationRecord {
			id = this@toRecord.id
			registeredClientId = this@toRecord.registeredClientId
			principalName = this@toRecord.principalName
			authorizationGrantType = this@toRecord.authorizationGrantType.value
			authorizedScopes = this@toRecord.authorizedScopes.joinValues()
			attributes = writeMap(this@toRecord.attributes)
			state = this@toRecord.getAttribute(OAuth2ParameterNames.STATE)
			authorizationCodeValue = authorizationCode?.token?.tokenValue
			authorizationCodeIssuedAt = authorizationCode?.token?.issuedAt.toOffsetDateTime()
			authorizationCodeExpiresAt = authorizationCode?.token?.expiresAt.toOffsetDateTime()
			authorizationCodeMetadata = writeMap(authorizationCode?.metadata.orEmpty())
			accessTokenValue = accessToken?.token?.tokenValue
			accessTokenIssuedAt = accessToken?.token?.issuedAt.toOffsetDateTime()
			accessTokenExpiresAt = accessToken?.token?.expiresAt.toOffsetDateTime()
			accessTokenMetadata = writeMap(accessToken?.metadata.orEmpty())
			accessTokenType = accessToken?.token?.tokenType?.value
			accessTokenScopes = accessToken?.token?.scopes.orEmpty().joinValues()
			oidcIdTokenValue = oidcIdToken?.token?.tokenValue
			oidcIdTokenIssuedAt = oidcIdToken?.token?.issuedAt.toOffsetDateTime()
			oidcIdTokenExpiresAt = oidcIdToken?.token?.expiresAt.toOffsetDateTime()
			oidcIdTokenMetadata = writeMap(oidcIdToken?.metadata.orEmpty())
			refreshTokenValue = refreshToken?.token?.tokenValue
			refreshTokenIssuedAt = refreshToken?.token?.issuedAt.toOffsetDateTime()
			refreshTokenExpiresAt = refreshToken?.token?.expiresAt.toOffsetDateTime()
			refreshTokenMetadata = writeMap(refreshToken?.metadata.orEmpty())
			userCodeValue = userCode?.token?.tokenValue
			userCodeIssuedAt = userCode?.token?.issuedAt.toOffsetDateTime()
			userCodeExpiresAt = userCode?.token?.expiresAt.toOffsetDateTime()
			userCodeMetadata = writeMap(userCode?.metadata.orEmpty())
			deviceCodeValue = deviceCode?.token?.tokenValue
			deviceCodeIssuedAt = deviceCode?.token?.issuedAt.toOffsetDateTime()
			deviceCodeExpiresAt = deviceCode?.token?.expiresAt.toOffsetDateTime()
			deviceCodeMetadata = writeMap(deviceCode?.metadata.orEmpty())
		}
	}

	private fun OAuth2AuthorizationRecord.toAuthorization(): OAuth2Authorization? {
		val registeredClient = registeredClientRepository.findById(registeredClientId) ?: return null
		val builder = OAuth2Authorization.withRegisteredClient(registeredClient)
			.id(id)
			.principalName(principalName)
			.authorizationGrantType(AuthorizationGrantType(authorizationGrantType))
			.authorizedScopes(authorizedScopes.splitValues())
			.attributes { values ->
				values.putAll(readMap(attributes))
			}

		authorizationCodeValue?.let { tokenValue ->
			val issuedAt = authorizationCodeIssuedAt?.toInstant() ?: return@let
			val expiresAt = authorizationCodeExpiresAt?.toInstant() ?: return@let
			builder.token(
				OAuth2AuthorizationCode(tokenValue, issuedAt, expiresAt),
			) { metadata ->
				metadata.putAll(readMap(authorizationCodeMetadata))
			}
		}
		accessTokenValue?.let { tokenValue ->
			val issuedAt = accessTokenIssuedAt?.toInstant() ?: return@let
			val expiresAt = accessTokenExpiresAt?.toInstant() ?: return@let
			builder.token(
				OAuth2AccessToken(
					accessTokenType.toAccessTokenType(),
					tokenValue,
					issuedAt,
					expiresAt,
					accessTokenScopes.splitValues(),
				),
			) { metadata ->
				metadata.putAll(readMap(accessTokenMetadata))
			}
		}
		oidcIdTokenValue?.let { tokenValue ->
			val issuedAt = oidcIdTokenIssuedAt?.toInstant() ?: return@let
			val expiresAt = oidcIdTokenExpiresAt?.toInstant() ?: return@let
			val metadata = readMap(oidcIdTokenMetadata)
			builder.token(
				OidcIdToken(tokenValue, issuedAt, expiresAt, metadata.claims()),
			) { values ->
				values.putAll(metadata)
			}
		}
		refreshTokenValue?.let { tokenValue ->
			val issuedAt = refreshTokenIssuedAt?.toInstant() ?: return@let
			builder.token(
				OAuth2RefreshToken(tokenValue, issuedAt, refreshTokenExpiresAt?.toInstant()),
			) { metadata ->
				metadata.putAll(readMap(refreshTokenMetadata))
			}
		}
		userCodeValue?.let { tokenValue ->
			val issuedAt = userCodeIssuedAt?.toInstant() ?: return@let
			val expiresAt = userCodeExpiresAt?.toInstant() ?: return@let
			builder.token(
				OAuth2UserCode(tokenValue, issuedAt, expiresAt),
			) { metadata ->
				metadata.putAll(readMap(userCodeMetadata))
			}
		}
		deviceCodeValue?.let { tokenValue ->
			val issuedAt = deviceCodeIssuedAt?.toInstant() ?: return@let
			val expiresAt = deviceCodeExpiresAt?.toInstant() ?: return@let
			builder.token(
				OAuth2DeviceCode(tokenValue, issuedAt, expiresAt),
			) { metadata ->
				metadata.putAll(readMap(deviceCodeMetadata))
			}
		}
		return builder.build()
	}

	private fun writeMap(values: Map<String, Any>): String? =
		values.takeIf { it.isNotEmpty() }
			?.mapValues { (key, value) ->
				if (key == Principal::class.java.name && value is Authentication) value.name else value.toJsonValue()
			}
			?.let(objectMapper::writeValueAsString)

	private fun readMap(value: String?): MutableMap<String, Any> {
		if (value.isNullOrBlank()) {
			return linkedMapOf()
		}
		return objectMapper.readValue(value, mapType)
			.restoreTokenClaims()
			.restoreAuthorizationPrincipal()
			.toMutableMap()
	}

	private fun Map<String, Any>.restoreAuthorizationPrincipal(): Map<String, Any> =
		mapValues { (key, value) ->
			if (key == Principal::class.java.name && value is String) {
				UsernamePasswordAuthenticationToken.authenticated(value, null, emptyList())
			} else value
		}

	@Suppress("UNCHECKED_CAST")
	private fun Map<String, Any>.claims(): Map<String, Any> =
		this[OAuth2Authorization.Token.CLAIMS_METADATA_NAME] as? Map<String, Any> ?: emptyMap()

	private fun Map<String, Any>.restoreTokenClaims(): Map<String, Any> =
		mapValues { (key, value) ->
			if (key == OAuth2Authorization.Token.CLAIMS_METADATA_NAME && value is Map<*, *>) {
				value.entries.associate { (claimName, claimValue) ->
					val normalizedName = claimName.toString()
					normalizedName to claimValue.restoreClaimValue(normalizedName)
				}
			} else {
				value
			}
		}

	private fun Any?.restoreClaimValue(claimName: String): Any? =
		if (claimName in INSTANT_CLAIM_NAMES && this is String) {
			runCatching { Instant.parse(this) }.getOrElse { this }
		} else {
			this
		}

	private fun Set<String>.joinValues(): String? =
		takeIf { it.isNotEmpty() }
			?.sorted()
			?.joinToString(VALUE_DELIMITER)

	private fun String?.splitValues(): Set<String> =
		this?.split(VALUE_DELIMITER)
			?.asSequence()
			?.map(String::trim)
			?.filter(String::isNotEmpty)
			?.toCollection(linkedSetOf())
			.orEmpty()

	private fun Instant?.toOffsetDateTime(): OffsetDateTime? =
		this?.atOffset(ZoneOffset.UTC)

	private fun String?.toAccessTokenType(): OAuth2AccessToken.TokenType =
		when (this) {
			OAuth2AccessToken.TokenType.DPOP.value -> OAuth2AccessToken.TokenType.DPOP
			else -> OAuth2AccessToken.TokenType.BEARER
		}

	private fun Any?.toJsonValue(): Any? =
		when (this) {
			null -> null
			is String, is Number, is Boolean -> this
			is Instant -> toString()
			is OffsetDateTime -> toString()
			is Map<*, *> -> entries.associate { (key, value) ->
				key.toString() to value.toJsonValue()
			}
			is Iterable<*> -> map { it.toJsonValue() }
			is Array<*> -> map { it.toJsonValue() }
			else -> toString()
		}

	private companion object {
		fun String.sha256(): String = HexFormat.of().formatHex(
			MessageDigest.getInstance("SHA-256").digest(toByteArray(Charsets.UTF_8)),
		)
		private const val VALUE_DELIMITER = " "
		private const val STATE_TOKEN_TYPE = "state"
		private const val CODE_TOKEN_TYPE = "code"
		private const val AUTHORIZATION_CODE_TOKEN_TYPE = "authorization_code"
		private const val ID_TOKEN_TYPE = "id_token"
		private const val USER_CODE_TOKEN_TYPE = "user_code"
		private const val DEVICE_CODE_TOKEN_TYPE = "device_code"
		private val INSTANT_CLAIM_NAMES = setOf("iat", "exp", "nbf", "auth_time")
		private val TOKEN_TYPE_VALUES = setOf(
			STATE_TOKEN_TYPE,
			CODE_TOKEN_TYPE,
			AUTHORIZATION_CODE_TOKEN_TYPE,
			OAuth2TokenType.ACCESS_TOKEN.value,
			ID_TOKEN_TYPE,
			OAuth2TokenType.REFRESH_TOKEN.value,
			USER_CODE_TOKEN_TYPE,
			DEVICE_CODE_TOKEN_TYPE,
		)
	}
}
