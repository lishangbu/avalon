package io.github.lishangbu.avalon.identity.access.interfaces.http.auth

import io.github.lishangbu.avalon.identity.access.domain.authentication.AuthenticatedSessionPrincipal
import io.github.lishangbu.avalon.identity.access.domain.authentication.AuthenticationUnauthorized
import io.github.lishangbu.avalon.identity.access.domain.iam.UserId
import org.eclipse.microprofile.jwt.JsonWebToken
import java.util.UUID

/**
 * 把当前访问链路上的 JWT 恢复为认证主体。
 *
 * 设计上 access token 只承载最小身份信息，因此这里只依赖 `sub` 和 `sid`
 * 两个 claim 来恢复当前用户和会话，不做额外数据库查询。
 *
 * @return 当前请求链路可直接交给认证应用服务的主体信息。
 */
internal fun JsonWebToken.toAuthenticatedSessionPrincipal(): AuthenticatedSessionPrincipal {
    val userId =
        subject?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?: throw AuthenticationUnauthorized("Authenticated subject is missing or invalid.")
    val sessionId =
        getClaim<String>("sid")?.trim()?.takeIf { it.isNotEmpty() }
            ?: throw AuthenticationUnauthorized("Authenticated session id is missing.")
    return AuthenticatedSessionPrincipal(UserId(userId), sessionId)
}
