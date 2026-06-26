package io.github.lishangbu.security.entity

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table
import java.time.OffsetDateTime

/**
 * Spring Authorization Server 标准授权记录表的 Jimmer 映射。
 *
 * 该表由 SAS 协议流程写入，字段形态保持标准 schema，运行时通过
 * [org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService]
 * 契约转换为 Spring Security 的授权模型。
 */
@Entity
@Table(name = "oauth2_authorization")
interface OAuth2AuthorizationRecord {
	@Id
	val id: String

	val registeredClientId: String
	val principalName: String
	val authorizationGrantType: String
	val authorizedScopes: String?
	val attributes: String?
	val state: String?
	val authorizationCodeValue: String?
	val authorizationCodeIssuedAt: OffsetDateTime?
	val authorizationCodeExpiresAt: OffsetDateTime?
	val authorizationCodeMetadata: String?
	val accessTokenValue: String?
	val accessTokenIssuedAt: OffsetDateTime?
	val accessTokenExpiresAt: OffsetDateTime?
	val accessTokenMetadata: String?
	val accessTokenType: String?
	val accessTokenScopes: String?
	val oidcIdTokenValue: String?
	val oidcIdTokenIssuedAt: OffsetDateTime?
	val oidcIdTokenExpiresAt: OffsetDateTime?
	val oidcIdTokenMetadata: String?
	val refreshTokenValue: String?
	val refreshTokenIssuedAt: OffsetDateTime?
	val refreshTokenExpiresAt: OffsetDateTime?
	val refreshTokenMetadata: String?
	val userCodeValue: String?
	val userCodeIssuedAt: OffsetDateTime?
	val userCodeExpiresAt: OffsetDateTime?
	val userCodeMetadata: String?
	val deviceCodeValue: String?
	val deviceCodeIssuedAt: OffsetDateTime?
	val deviceCodeExpiresAt: OffsetDateTime?
	val deviceCodeMetadata: String?
}
