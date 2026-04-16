package io.github.lishangbu.avalon.identity.access.infrastructure.authentication

import io.github.lishangbu.avalon.identity.access.domain.authentication.AuthenticationUser
import io.smallrye.jwt.build.Jwt
import jakarta.enterprise.context.ApplicationScoped
import java.time.Duration
import java.time.Instant
import org.eclipse.microprofile.config.inject.ConfigProperty

/**
 * 已签发的 access token 结果。
 *
 * @property token JWT 文本。
 * @property expiresAt access token 的绝对过期时间。
 */
data class IssuedAccessToken(
    val token: String,
    val expiresAt: Instant,
)

/**
 * JWT access token 签发器。
 *
 * 当前阶段 access token 只承载最小身份 claim，把角色、权限和菜单快照继续留在
 * IAM 读取链路中，避免 token 体积膨胀且难以及时失效。
 */
@ApplicationScoped
class JwtAccessTokenIssuer(
    private val opaqueTokenService: OpaqueTokenService,
    @param:ConfigProperty(name = "avalon.auth.issuer") private val issuer: String,
    @param:ConfigProperty(name = "avalon.auth.access-token-ttl") private val accessTokenTtl: Duration,
) {
    /**
     * 为指定用户和会话签发 access token。
     *
     * @param user 已通过认证的用户主体。
     * @param sessionId 当前会话标识。
     * @param issuedAt token 签发时间。
     * @return 包含 JWT 文本和过期时间的结果对象。
     */
    fun issue(
        user: AuthenticationUser,
        sessionId: String,
        issuedAt: Instant,
    ): IssuedAccessToken {
        // 第一阶段 access token 只携带最小身份 claim；角色、权限、菜单快照仍通过 IAM 读取，避免 token 过胖且难失效。
        val expiresAt = issuedAt.plus(accessTokenTtl)
        val token =
            Jwt.claims()
                .issuer(issuer)
                .subject(user.id.value.toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("sid", sessionId)
                .claim("preferred_username", user.username)
                .claim("jti", opaqueTokenService.generateIdentifier(24))
                .claim("typ", "access")
                .sign()
        return IssuedAccessToken(token = token, expiresAt = expiresAt)
    }
}