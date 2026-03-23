package io.github.lishangbu.avalon.authorization.entity

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table
import java.time.Instant

@Entity
@Table(name = "oauth_registered_client")
interface OauthRegisteredClient {
    /** ID */
    @Id
    val id: String

    /** 客户端 ID */
    val clientId: String?

    /** 客户端 ID 签发时间 */
    val clientIdIssuedAt: Instant?

    /** 客户端密钥 */
    val clientSecret: String?

    /** 客户端密钥过期时间 */
    val clientSecretExpiresAt: Instant?

    /** 客户端名称 */
    val clientName: String?

    /** 客户端认证方法列表 */
    val clientAuthenticationMethods: String?

    /** 授权授权属性列表 */
    val authorizationGrantTypes: String?

    /** 重定向 URI 列表 */
    val redirectUris: String?

    /** 登出后重定向 URI 列表 */
    val postLogoutRedirectUris: String?

    /** 权限范围列表 */
    val scopes: String?

    /** 要求 PKCE */
    val requireProofKey: Boolean?

    /** 要求授权授权同意 */
    val requireAuthorizationConsent: Boolean?

    /** JWK Set URL */
    val jwkSetUrl: String?

    /** 令牌端点认证签名算法 */
    val tokenEndpointAuthenticationSigningAlgorithm: String?

    /** X509 证书主题标识 */
    val x509CertificateSubjectDn: String?

    /** 授权状态码有效期 */
    val authorizationCodeTimeToLive: String?

    /** 访问令牌有效期 */
    val accessTokenTimeToLive: String?

    /** 访问令牌格式 */
    val accessTokenFormat: String?

    /** 设备状态码有效期 */
    val deviceCodeTimeToLive: String?

    /** 复用刷新令牌 */
    val reuseRefreshTokens: Boolean?

    /** 刷新令牌有效期 */
    val refreshTokenTimeToLive: String?

    /** ID 令牌签名算法 */
    val idTokenSignatureAlgorithm: String?

    /** X509 证书绑定访问令牌列表 */
    val x509CertificateBoundAccessTokens: Boolean?
}
