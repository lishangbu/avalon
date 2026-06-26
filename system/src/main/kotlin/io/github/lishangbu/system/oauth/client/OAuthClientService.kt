package io.github.lishangbu.system.oauth.client

import io.github.lishangbu.security.entity.OAuth2Client
import io.github.lishangbu.security.entity.accessTokenFormat
import io.github.lishangbu.security.entity.clientId
import io.github.lishangbu.security.entity.clientName
import io.github.lishangbu.security.entity.id
import io.github.lishangbu.security.oauth.PASSWORD_GRANT_TYPE_VALUE
import io.github.lishangbu.security.rbac.SECURITY_ADMIN_ACCESS_NODE
import io.github.lishangbu.security.repository.OAuth2ClientRepository
import io.github.lishangbu.system.error.conflict
import io.github.lishangbu.system.error.invalidValue
import io.github.lishangbu.system.error.notFound
import io.github.lishangbu.system.error.normalizedAccessNodeCodes
import io.github.lishangbu.system.error.requireSupportedValues
import io.github.lishangbu.system.error.requiredRange
import io.github.lishangbu.system.error.requiredSlugCode
import io.github.lishangbu.system.error.requiredSupportedValue
import io.github.lishangbu.system.error.requiredText
import io.github.lishangbu.system.page.mapRows
import io.github.lishangbu.system.page.systemSearchFilter
import io.github.lishangbu.system.page.validateSystemPage
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.ilike
import org.babyfish.jimmer.sql.kt.ast.expression.or
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * OAuth client 系统管理服务。
 *
 * 管理域只维护可由后台配置的 client 元数据，SAS 运行时读取仍由 security 模块负责。
 */
@Service
@ConditionalOnProperty(prefix = "backend.security", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class OAuthClientService(
	private val clientRepository: OAuth2ClientRepository,
	private val sqlClient: KSqlClient,
) {
	/**
	 * 查询 OAuth client 元数据，响应会隐藏 secret。
	 */
	@Transactional(readOnly = true)
	fun listClients(page: Int, size: Int, query: String?): Page<OAuthClientResponse> {
		validateSystemPage(page, size)
		val searchFilter = systemSearchFilter(query)
		return sqlClient.createQuery(OAuth2Client::class) {
			searchFilter.pattern?.let { pattern ->
				where(
					or(
						table.clientId ilike pattern,
						table.clientName ilike pattern,
						table.accessTokenFormat ilike pattern,
					),
				)
			}
			orderBy(table.clientId)
			select(table)
		}.fetchPage(page, size)
			.mapRows { it.toResponse() }
	}

	/**
	 * 查询单个 OAuth client 元数据。
	 */
	@Transactional(readOnly = true)
	fun getClient(clientId: String): OAuthClientResponse =
		clientByClientIdOrNotFound(clientId).toResponse()

	/**
	 * 创建 password grant 管理客户端。
	 */
	@Transactional
	fun createClient(request: CreateOAuthClientRequest): OAuthClientResponse {
		val clientId = request.clientId.requiredSlugCode("clientId")
		if (clientExists(clientId)) {
			conflict("clientId", "clientId 已存在")
		}

		val scopes = request.scopes.normalizedAccessNodeCodes("scopes")
		scopes.requireSupportedValues("scopes", SUPPORTED_SCOPES)
		val accessTokenFormat = request.accessTokenFormat.requiredSupportedValue("accessTokenFormat", ACCESS_TOKEN_FORMATS)
		val clientSecret = request.clientSecret.orEmpty()
			.requiredText("clientSecret", maxLength = 200, minLength = MIN_CLIENT_SECRET_LENGTH)
		val clientName = request.clientName.requiredText("clientName", maxLength = 120)

		val now = OffsetDateTime.now(ZoneOffset.UTC)
		return clientRepository.save(
			OAuth2Client {
				this.clientId = clientId
				clientIdIssuedAt = now
				this.clientSecret = clientSecret
				clientSecretExpiresAt = null
				this.clientName = clientName
				clientAuthenticationMethods = CLIENT_SECRET_BASIC
				authorizationGrantTypes = PASSWORD_GRANT_TYPE_VALUE
				redirectUris = null
				postLogoutRedirectUris = null
				this.scopes = scopes.joinToString(VALUE_DELIMITER)
				requireProofKey = true
				requireAuthorizationConsent = false
				jwkSetUrl = null
				tokenEndpointAuthenticationSigningAlgorithm = null
				x509CertificateSubjectDn = null
				authorizationCodeTtlSeconds = AUTHORIZATION_CODE_TTL_SECONDS
				accessTokenTtlSeconds = ACCESS_TOKEN_TTL_SECONDS
				this.accessTokenFormat = accessTokenFormat
				deviceCodeTtlSeconds = DEVICE_CODE_TTL_SECONDS
				reuseRefreshTokens = true
				refreshTokenTtlSeconds = REFRESH_TOKEN_TTL_SECONDS
				idTokenSignatureAlgorithm = ID_TOKEN_SIGNATURE_ALGORITHM
				x509CertificateBoundAccessTokens = false
			},
		).toResponse()
	}

	/**
	 * 更新 OAuth client 可管理字段，并保留 SAS 协议基线字段。
	 */
	@Transactional
	fun updateClient(clientId: String, request: UpdateOAuthClientRequest): OAuthClientResponse {
		val client = clientByClientIdOrNotFound(clientId)
		val scopes = request.scopes.normalizedAccessNodeCodes("scopes")
		scopes.requireSupportedValues("scopes", SUPPORTED_SCOPES)
		val accessTokenFormat = request.accessTokenFormat.requiredSupportedValue("accessTokenFormat", ACCESS_TOKEN_FORMATS)
		val accessTokenTtlSeconds = request.accessTokenTtlSeconds.requiredRange(
			fieldName = "accessTokenTtlSeconds",
			min = MIN_ACCESS_TOKEN_TTL_SECONDS,
			max = MAX_ACCESS_TOKEN_TTL_SECONDS,
		)
		val refreshTokenTtlSeconds = request.refreshTokenTtlSeconds.requiredRange(
			fieldName = "refreshTokenTtlSeconds",
			min = MIN_REFRESH_TOKEN_TTL_SECONDS,
			max = MAX_REFRESH_TOKEN_TTL_SECONDS,
		)
		if (refreshTokenTtlSeconds < accessTokenTtlSeconds) {
			invalidValue("refreshTokenTtlSeconds", "refreshTokenTtlSeconds 不能小于 accessTokenTtlSeconds")
		}
		return saveClient(
			client = client,
			clientName = request.clientName.requiredText("clientName", maxLength = 120),
			scopes = scopes,
			accessTokenFormat = accessTokenFormat,
			accessTokenTtlSeconds = accessTokenTtlSeconds,
			refreshTokenTtlSeconds = refreshTokenTtlSeconds,
		).toResponse()
	}

	/**
	 * 重置 OAuth client secret，响应仍不回显 secret。
	 */
	@Transactional
	fun resetClientSecret(clientId: String, request: ResetOAuthClientSecretRequest): OAuthClientResponse {
		val client = clientByClientIdOrNotFound(clientId)
		val clientSecret = request.clientSecret.orEmpty()
			.requiredText("clientSecret", maxLength = 200, minLength = MIN_CLIENT_SECRET_LENGTH)
		return saveClient(client, clientSecret = clientSecret).toResponse()
	}

	/**
	 * 按 clientId 查询 OAuth client，不存在时返回稳定 404。
	 */
	private fun clientByClientIdOrNotFound(clientId: String): OAuth2Client =
		findEntityByClientId(clientId)
			?: notFound("clientId", "clientId 不存在: $clientId")

	/**
	 * 使用数据库唯一键前置检查，给管理端返回更明确的冲突错误。
	 */
	private fun clientExists(clientId: String): Boolean =
		sqlClient.createQuery(OAuth2Client::class) {
			where(table.clientId eq clientId)
			select(table.id)
		}.exists()

	/**
	 * 按 clientId 读取持久化 client。
	 */
	private fun findEntityByClientId(clientId: String): OAuth2Client? =
		sqlClient.executeQuery(OAuth2Client::class, limit = 1) {
			where(table.clientId eq clientId)
			select(table)
		}.firstOrNull()

	/**
	 * 保存 OAuth client 变更，固定字段沿用创建时的协议基线。
	 */
	private fun saveClient(
		client: OAuth2Client,
		clientSecret: String? = client.clientSecret,
		clientName: String = client.clientName,
		scopes: List<String> = client.scopes.splitValues(),
		accessTokenFormat: String = client.accessTokenFormat,
		accessTokenTtlSeconds: Long = client.accessTokenTtlSeconds,
		refreshTokenTtlSeconds: Long = client.refreshTokenTtlSeconds,
	): OAuth2Client =
		clientRepository.save(
			OAuth2Client {
				id = client.id
				clientId = client.clientId
				clientIdIssuedAt = client.clientIdIssuedAt
				this.clientSecret = clientSecret
				clientSecretExpiresAt = client.clientSecretExpiresAt
				this.clientName = clientName
				clientAuthenticationMethods = client.clientAuthenticationMethods
				authorizationGrantTypes = client.authorizationGrantTypes
				redirectUris = client.redirectUris
				postLogoutRedirectUris = client.postLogoutRedirectUris
				this.scopes = scopes.joinToString(VALUE_DELIMITER)
				requireProofKey = client.requireProofKey
				requireAuthorizationConsent = client.requireAuthorizationConsent
				jwkSetUrl = client.jwkSetUrl
				tokenEndpointAuthenticationSigningAlgorithm = client.tokenEndpointAuthenticationSigningAlgorithm
				x509CertificateSubjectDn = client.x509CertificateSubjectDn
				authorizationCodeTtlSeconds = client.authorizationCodeTtlSeconds
				this.accessTokenTtlSeconds = accessTokenTtlSeconds
				this.accessTokenFormat = accessTokenFormat
				deviceCodeTtlSeconds = client.deviceCodeTtlSeconds
				reuseRefreshTokens = client.reuseRefreshTokens
				this.refreshTokenTtlSeconds = refreshTokenTtlSeconds
				idTokenSignatureAlgorithm = client.idTokenSignatureAlgorithm
				x509CertificateBoundAccessTokens = client.x509CertificateBoundAccessTokens
			},
		)

	/**
	 * 将持久化 client 转换为不含 secret 的响应。
	 */
	private fun OAuth2Client.toResponse(): OAuthClientResponse =
		OAuthClientResponse(
			id = id,
			clientId = clientId,
			clientName = clientName,
			clientAuthenticationMethods = clientAuthenticationMethods.splitValues(),
			authorizationGrantTypes = authorizationGrantTypes.splitValues(),
			scopes = scopes.splitValues(),
			accessTokenFormat = accessTokenFormat,
			accessTokenTtlSeconds = accessTokenTtlSeconds,
			refreshTokenTtlSeconds = refreshTokenTtlSeconds,
		)

	/**
	 * 拆分以空格持久化的 Spring Authorization Server 多值字段。
	 */
	private fun String.splitValues(): List<String> =
		split(VALUE_DELIMITER)
			.map(String::trim)
			.filter(String::isNotEmpty)

	private companion object {
		private const val VALUE_DELIMITER = " "
		private const val CLIENT_SECRET_BASIC = "client_secret_basic"
		private const val AUTHORIZATION_CODE_TTL_SECONDS = 300L
		private const val ACCESS_TOKEN_TTL_SECONDS = 1800L
		private const val DEVICE_CODE_TTL_SECONDS = 300L
		private const val REFRESH_TOKEN_TTL_SECONDS = 604800L
		private const val MIN_ACCESS_TOKEN_TTL_SECONDS = 60L
		private const val MAX_ACCESS_TOKEN_TTL_SECONDS = 86400L
		private const val MIN_REFRESH_TOKEN_TTL_SECONDS = 300L
		private const val MAX_REFRESH_TOKEN_TTL_SECONDS = 2592000L
		private const val MIN_CLIENT_SECRET_LENGTH = 8
		private const val ID_TOKEN_SIGNATURE_ALGORITHM = "RS256"
		private val ACCESS_TOKEN_FORMATS = setOf("self-contained", "reference")
		private val SUPPORTED_SCOPES = setOf(SECURITY_ADMIN_ACCESS_NODE)
	}
}
