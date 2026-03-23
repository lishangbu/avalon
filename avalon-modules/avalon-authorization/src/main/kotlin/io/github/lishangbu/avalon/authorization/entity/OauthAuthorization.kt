package io.github.lishangbu.avalon.authorization.entity

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table
import java.time.Instant

@Entity
@Table(name = "oauth_authorization")
interface OauthAuthorization {
    /** ID */
    @Id
    val id: String

    /** 注册客户端 ID */
    val registeredClientId: String?

    /** 主体名称 */
    val principalName: String?

    /** 授权类型 */
    val authorizationGrantType: String?

    /** 已授权权限范围 */
    val authorizedScopes: String?

    /** 属性映射 */
    val attributes: String?

    /** 状态 */
    val state: String?

    /** 授权状态码值 */
    val authorizationCodeValue: String?

    /** 授权状态码签发时间 */
    val authorizationCodeIssuedAt: Instant?

    /** 授权状态码过期时间 */
    val authorizationCodeExpiresAt: Instant?

    /** 授权状态码元数据 */
    val authorizationCodeMetadata: String?

    /** 访问令牌值 */
    val accessTokenValue: String?

    /** 访问令牌签发时间 */
    val accessTokenIssuedAt: Instant?

    /** 访问令牌过期时间 */
    val accessTokenExpiresAt: Instant?

    /** 访问令牌元数据 */
    val accessTokenMetadata: String?

    /** 访问令牌属性 */
    val accessTokenType: String?

    /** 访问令牌权限范围列表 */
    val accessTokenScopes: String?

    /** OIDC ID 令牌值 */
    val oidcIdTokenValue: String?

    /** OIDC ID 令牌签发时间 */
    val oidcIdTokenIssuedAt: Instant?

    /** OIDC ID 令牌过期时间 */
    val oidcIdTokenExpiresAt: Instant?

    /** OIDC ID 令牌元数据 */
    val oidcIdTokenMetadata: String?

    /** 刷新令牌值 */
    val refreshTokenValue: String?

    /** 刷新令牌签发时间 */
    val refreshTokenIssuedAt: Instant?

    /** 刷新令牌过期时间 */
    val refreshTokenExpiresAt: Instant?

    /** 刷新令牌元数据 */
    val refreshTokenMetadata: String?

    /** 用户状态码值 */
    val userCodeValue: String?

    /** 用户状态码签发时间 */
    val userCodeIssuedAt: Instant?

    /** 用户状态码过期时间 */
    val userCodeExpiresAt: Instant?

    /** 用户状态码元数据 */
    val userCodeMetadata: String?

    /** 设备状态码值 */
    val deviceCodeValue: String?

    /** 设备状态码签发时间 */
    val deviceCodeIssuedAt: Instant?

    /** 设备状态码过期时间 */
    val deviceCodeExpiresAt: Instant?

    /** 设备状态码元数据 */
    val deviceCodeMetadata: String?
}
