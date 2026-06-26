package io.github.lishangbu.security.repository

import io.github.lishangbu.security.entity.OAuth2Client
import io.github.lishangbu.security.entity.clientId
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.jose.jws.JwsAlgorithm
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * 基于 Jimmer 实体的 Spring Authorization Server 客户端仓库。
 *
 * SAS 运行时只依赖 [RegisteredClientRepository] 契约；本实现负责在 SAS 模型和
 * [OAuth2Client] 的拍平列之间转换，避免使用 SAS JDBC client 表及其 JSON settings
 * 字段。
 */
class JimmerRegisteredClientRepository(
	private val clientRepository: OAuth2ClientRepository,
	private val sqlClient: KSqlClient,
) : RegisteredClientRepository {
	override fun save(registeredClient: RegisteredClient) {
		val existingClient = findEntityByClientId(registeredClient.clientId)
		val now = OffsetDateTime.now(ZoneOffset.UTC)
		clientRepository.save(
			OAuth2Client {
				val persistedId = existingClient?.id ?: registeredClient.id.toLongOrNull()
				if (persistedId != null) {
					id = persistedId
				}
				clientId = registeredClient.clientId
				clientIdIssuedAt = registeredClient.clientIdIssuedAt.toOffsetDateTimeOrNow(now)
				clientSecret = registeredClient.clientSecret
				clientSecretExpiresAt = registeredClient.clientSecretExpiresAt.toOffsetDateTime()
				clientName = registeredClient.clientName
				clientAuthenticationMethods = registeredClient.clientAuthenticationMethods.joinValues { it.value }
				authorizationGrantTypes = registeredClient.authorizationGrantTypes.joinValues { it.value }
				redirectUris = registeredClient.redirectUris.joinNullableValues()
				postLogoutRedirectUris = registeredClient.postLogoutRedirectUris.joinNullableValues()
				scopes = registeredClient.scopes.joinValues { it }
				requireProofKey = registeredClient.clientSettings.isRequireProofKey
				requireAuthorizationConsent = registeredClient.clientSettings.isRequireAuthorizationConsent
				jwkSetUrl = registeredClient.clientSettings.jwkSetUrl
				tokenEndpointAuthenticationSigningAlgorithm =
					registeredClient.clientSettings.tokenEndpointAuthenticationSigningAlgorithm?.name
				x509CertificateSubjectDn = registeredClient.clientSettings.x509CertificateSubjectDN
				authorizationCodeTtlSeconds = registeredClient.tokenSettings.authorizationCodeTimeToLive.seconds
				accessTokenTtlSeconds = registeredClient.tokenSettings.accessTokenTimeToLive.seconds
				accessTokenFormat = registeredClient.tokenSettings.accessTokenFormat.value
				deviceCodeTtlSeconds = registeredClient.tokenSettings.deviceCodeTimeToLive.seconds
				reuseRefreshTokens = registeredClient.tokenSettings.isReuseRefreshTokens
				refreshTokenTtlSeconds = registeredClient.tokenSettings.refreshTokenTimeToLive.seconds
				idTokenSignatureAlgorithm = registeredClient.tokenSettings.idTokenSignatureAlgorithm.name
				x509CertificateBoundAccessTokens =
					registeredClient.tokenSettings.isX509CertificateBoundAccessTokens
			},
		)
	}

	override fun findById(id: String): RegisteredClient? =
		id.toLongOrNull()
			?.let(clientRepository::findNullable)
			?.toRegisteredClient()

	override fun findByClientId(clientId: String): RegisteredClient? =
		findEntityByClientId(clientId)?.toRegisteredClient()

	private fun findEntityByClientId(clientId: String): OAuth2Client? =
		sqlClient.executeQuery(OAuth2Client::class, limit = 1) {
			where(table.clientId eq clientId)
			select(table)
		}.firstOrNull()

	private fun OAuth2Client.toRegisteredClient(): RegisteredClient {
		val builder = RegisteredClient.withId(id.toString())
			.clientId(clientId)
			.clientIdIssuedAt(clientIdIssuedAt.toInstant())
			.clientName(clientName)
			.clientAuthenticationMethods { methods ->
				methods.addAll(clientAuthenticationMethods.splitValues().map(ClientAuthenticationMethod::valueOf))
			}
			.authorizationGrantTypes { grantTypes ->
				grantTypes.addAll(authorizationGrantTypes.splitValues().map(::AuthorizationGrantType))
			}
			.redirectUris { values ->
				values.addAll(redirectUris.splitValues())
			}
			.postLogoutRedirectUris { values ->
				values.addAll(postLogoutRedirectUris.splitValues())
			}
			.scopes { values ->
				values.addAll(scopes.splitValues())
			}
			.clientSettings(toClientSettings())
			.tokenSettings(toTokenSettings())

		clientSecret?.let(builder::clientSecret)
		clientSecretExpiresAt?.let { builder.clientSecretExpiresAt(it.toInstant()) }
		return builder.build()
	}

	private fun OAuth2Client.toClientSettings(): ClientSettings {
		val builder = ClientSettings.builder()
			.requireProofKey(requireProofKey)
			.requireAuthorizationConsent(requireAuthorizationConsent)

		jwkSetUrl?.let(builder::jwkSetUrl)
		tokenEndpointAuthenticationSigningAlgorithm
			?.let(::resolveJwsAlgorithm)
			?.let(builder::tokenEndpointAuthenticationSigningAlgorithm)
		x509CertificateSubjectDn?.let(builder::x509CertificateSubjectDN)
		return builder.build()
	}

	private fun OAuth2Client.toTokenSettings(): TokenSettings =
		TokenSettings.builder()
			.authorizationCodeTimeToLive(Duration.ofSeconds(authorizationCodeTtlSeconds))
			.accessTokenTimeToLive(Duration.ofSeconds(accessTokenTtlSeconds))
			.accessTokenFormat(OAuth2TokenFormat(accessTokenFormat))
			.deviceCodeTimeToLive(Duration.ofSeconds(deviceCodeTtlSeconds))
			.reuseRefreshTokens(reuseRefreshTokens)
			.refreshTokenTimeToLive(Duration.ofSeconds(refreshTokenTtlSeconds))
			.idTokenSignatureAlgorithm(SignatureAlgorithm.from(idTokenSignatureAlgorithm) ?: SignatureAlgorithm.RS256)
			.x509CertificateBoundAccessTokens(x509CertificateBoundAccessTokens)
			.build()

	private fun resolveJwsAlgorithm(value: String): JwsAlgorithm =
		SignatureAlgorithm.from(value) ?: MacAlgorithm.from(value) ?: error("Unsupported JWS algorithm: $value")

	private fun Set<String>.joinNullableValues(): String? =
		takeIf { it.isNotEmpty() }?.joinValues { it }

	private fun <T> Set<T>.joinValues(selector: (T) -> String): String =
		map(selector).sorted().joinToString(VALUE_DELIMITER)

	private fun String?.splitValues(): Set<String> =
		this?.split(VALUE_DELIMITER)
			?.asSequence()
			?.map(String::trim)
			?.filter(String::isNotEmpty)
			?.toSet()
			.orEmpty()

	private fun Instant?.toOffsetDateTime(): OffsetDateTime? =
		this?.atOffset(ZoneOffset.UTC)

	private fun Instant?.toOffsetDateTimeOrNow(now: OffsetDateTime): OffsetDateTime =
		toOffsetDateTime() ?: now

	private companion object {
		private const val VALUE_DELIMITER = " "
	}
}
