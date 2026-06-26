package io.github.lishangbu.security.entity

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.sql.Column
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Key
import org.babyfish.jimmer.sql.Table
import java.time.OffsetDateTime

/**
 * 授权服务器客户端注册信息。
 *
 * 该实体是 Backend 自有的客户端持久化模型，不复用 Spring Authorization Server
 * JDBC schema。`clientSettings` 与 `tokenSettings` 的标准字段在表中拍平，便于后续
 * 管理端按列检索、审计和变更。
 */
@Entity
@Table(name = "oauth2_client")
interface OAuth2Client {
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	val id: Long

	@Key
	val clientId: String

	val clientIdIssuedAt: OffsetDateTime
	val clientSecret: String?
	val clientSecretExpiresAt: OffsetDateTime?
	val clientName: String
	val clientAuthenticationMethods: String
	val authorizationGrantTypes: String
	val redirectUris: String?
	val postLogoutRedirectUris: String?
	val scopes: String
	val requireProofKey: Boolean
	val requireAuthorizationConsent: Boolean
	val jwkSetUrl: String?
	val tokenEndpointAuthenticationSigningAlgorithm: String?

	@Column(name = "x509_certificate_subject_dn")
	val x509CertificateSubjectDn: String?

	val authorizationCodeTtlSeconds: Long
	val accessTokenTtlSeconds: Long
	val accessTokenFormat: String
	val deviceCodeTtlSeconds: Long
	val reuseRefreshTokens: Boolean
	val refreshTokenTtlSeconds: Long
	val idTokenSignatureAlgorithm: String

	@Column(name = "x509_certificate_bound_access_tokens")
	val x509CertificateBoundAccessTokens: Boolean

	val createdAt: OffsetDateTime
	val updatedAt: OffsetDateTime
}
