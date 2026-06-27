package io.github.lishangbu.system.service

import io.github.lishangbu.system.dto.OAuthTokenResponse
import io.github.lishangbu.system.dto.OAuthTokenStatus
import io.github.lishangbu.common.web.filterValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.security.entity.OAuth2AuthorizationRecord
import io.github.lishangbu.security.entity.OAuth2Client
import io.github.lishangbu.security.entity.accessTokenIssuedAt
import io.github.lishangbu.security.entity.accessTokenScopes
import io.github.lishangbu.security.entity.authorizationGrantType
import io.github.lishangbu.security.entity.authorizedScopes
import io.github.lishangbu.security.entity.clientId
import io.github.lishangbu.security.entity.id
import io.github.lishangbu.security.entity.principalName
import io.github.lishangbu.security.entity.registeredClientId
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.asc
import org.babyfish.jimmer.sql.kt.ast.expression.desc
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.ilike
import org.babyfish.jimmer.sql.kt.ast.expression.or
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * OAuth 令牌系统管理服务。
 */
@Service
@ConditionalOnProperty(prefix = "backend.security", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class OAuthTokenService(
	private val sqlClient: KSqlClient,
	private val authorizationService: OAuth2AuthorizationService,
) {
	/**
	 * 分页查询授权记录和令牌状态。
	 */
	@Transactional(readOnly = true)
	fun listTokens(
		page: Int,
		size: Int,
		query: String?,
		clientId: String?,
		principalName: String?,
	): Page<OAuthTokenResponse> {
		validatePage(page, size)
		val searchFilter = searchFilter(query)
		val clientRecordId = filterValue("clientId", clientId)
			?.let(::registeredClientIdByClientId)
		val principalNameFilter = filterValue("principalName", principalName)
		return sqlClient.createQuery(OAuth2AuthorizationRecord::class) {
			searchFilter.pattern?.let { pattern ->
				where(
					or(
						table.id ilike pattern,
						table.registeredClientId ilike pattern,
						table.principalName ilike pattern,
						table.authorizationGrantType ilike pattern,
						table.authorizedScopes ilike pattern,
						table.accessTokenScopes ilike pattern,
					),
				)
			}
			clientRecordId?.let { value ->
				where(table.registeredClientId eq value)
			}
			principalNameFilter?.let { value ->
				where(table.principalName eq value)
			}
			orderBy(table.accessTokenIssuedAt.desc(), table.id.asc())
			select(table)
		}.fetchPage(page, size)
			.mapRows { it.toResponse() }
	}

	/**
	 * 查询单个授权记录和令牌状态。
	 */
	@Transactional(readOnly = true)
	fun getToken(authorizationId: String): OAuthTokenResponse =
		authorizationRecordOrNotFound(authorizationId).toResponse()

	/**
	 * 撤销授权记录中的 access token 和 refresh token。
	 */
	@Transactional
	fun revokeToken(authorizationId: String): OAuthTokenResponse {
		authorizationRecordOrNotFound(authorizationId)
		val authorization = authorizationService.findById(authorizationId)
			?: notFound("authorizationId", "授权记录不存在: $authorizationId")
		val builder = OAuth2Authorization.from(authorization)
		authorization.accessToken?.token?.let { builder.invalidate(it) }
		authorization.refreshToken?.token?.let { builder.invalidate(it) }
		authorizationService.save(builder.build())
		return getToken(authorizationId)
	}

	private fun authorizationRecordOrNotFound(authorizationId: String): OAuth2AuthorizationRecord =
		sqlClient.executeQuery(OAuth2AuthorizationRecord::class, limit = 1) {
			where(table.id eq authorizationId)
			select(table)
		}
			.firstOrNull()
			?: notFound("authorizationId", "授权记录不存在: $authorizationId")

	private fun OAuth2AuthorizationRecord.toResponse(): OAuthTokenResponse {
		val authorization = authorizationService.findById(id)
		val client = clientByRegisteredClientId(registeredClientId)
		val accessToken = authorization?.accessToken
		val refreshToken = authorization?.refreshToken
		return OAuthTokenResponse(
			id = id,
			registeredClientId = registeredClientId,
			clientId = client?.clientId,
			clientName = client?.clientName,
			principalName = principalName,
			authorizationGrantType = authorizationGrantType,
			authorizedScopes = authorization?.authorizedScopes?.sorted() ?: authorizedScopes.splitValues(),
			accessTokenScopes = accessToken?.token?.scopes?.sorted() ?: accessTokenScopes.splitValues(),
			accessTokenType = accessToken?.token?.tokenType?.value,
			accessTokenIssuedAt = accessToken?.token?.issuedAt.toOffsetDateTime(),
			accessTokenExpiresAt = accessToken?.token?.expiresAt.toOffsetDateTime(),
			refreshTokenIssuedAt = refreshToken?.token?.issuedAt.toOffsetDateTime(),
			refreshTokenExpiresAt = refreshToken?.token?.expiresAt.toOffsetDateTime(),
			status = accessToken.status(),
			active = accessToken?.isActive == true,
		)
	}

	private fun registeredClientIdByClientId(clientId: String): String =
		sqlClient.executeQuery(OAuth2Client::class, limit = 1) {
			where(table.clientId eq clientId)
			select(table.id)
		}
			.firstOrNull()
			?.toString()
			?: MISSING_REGISTERED_CLIENT_ID

	private fun clientByRegisteredClientId(registeredClientId: String): OAuth2Client? {
		val clientRecordId = registeredClientId.toLongOrNull() ?: return null
		return sqlClient.executeQuery(OAuth2Client::class, limit = 1) {
			where(table.id eq clientRecordId)
			select(table)
		}.firstOrNull()
	}

	private fun OAuth2Authorization.Token<OAuth2AccessToken>?.status(): OAuthTokenStatus =
		when {
			this == null -> OAuthTokenStatus.NO_ACCESS_TOKEN
			isInvalidated -> OAuthTokenStatus.REVOKED
			isExpired -> OAuthTokenStatus.EXPIRED
			else -> OAuthTokenStatus.ACTIVE
		}

	private fun Instant?.toOffsetDateTime(): OffsetDateTime? =
		this?.atOffset(ZoneOffset.UTC)

	private fun String?.splitValues(): List<String> =
		this?.split(VALUE_DELIMITER)
			?.asSequence()
			?.map(String::trim)
			?.filter(String::isNotEmpty)
			?.sorted()
			?.toList()
			.orEmpty()

	private companion object {
		private const val VALUE_DELIMITER = " "
		private const val MISSING_REGISTERED_CLIENT_ID = "__missing_registered_client_id__"
	}
}
