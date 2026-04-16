package io.github.lishangbu.avalon.identity.access.application.authentication

import io.github.lishangbu.avalon.identity.access.application.iam.IdentityAccessService
import io.github.lishangbu.avalon.identity.access.domain.authentication.*
import io.github.lishangbu.avalon.identity.access.infrastructure.authentication.BcryptPasswordHasher
import io.github.lishangbu.avalon.identity.access.infrastructure.authentication.JwtAccessTokenIssuer
import io.github.lishangbu.avalon.identity.access.infrastructure.authentication.OpaqueTokenService
import io.github.lishangbu.avalon.shared.application.time.ClockProvider
import jakarta.enterprise.context.ApplicationScoped
import java.time.Duration
import org.eclipse.microprofile.config.inject.ConfigProperty

@ApplicationScoped
/**
 * 本服务只负责本地认证用例编排：
 * 1. 认证凭证校验
 * 2. 会话与 refresh token 生命周期编排
 * 3. 审计日志记录
 *
 * 它不负责把完整权限快照塞进 access token，也不做每请求在线会话校验。
 */
class AuthenticationService(
    private val authenticationRepository: AuthenticationRepository,
    private val identityAccessService: IdentityAccessService,
    private val passwordHasher: BcryptPasswordHasher,
    private val accessTokenIssuer: JwtAccessTokenIssuer,
    private val opaqueTokenService: OpaqueTokenService,
    private val clockProvider: ClockProvider,
    @param:ConfigProperty(name = "avalon.auth.refresh-token-ttl") private val refreshTokenTtl: Duration,
    @param:ConfigProperty(name = "avalon.auth.max-admin-sessions") private val maxAdminSessions: Int,
) {
    /**
     * 执行用户名、邮箱或手机号登录。
     *
     * 设计上这个方法只把最小身份信息写入 access token，refresh token 明文只在响应阶段返回一次，
     * 仓储层只落 hash，以降低长期持有明文令牌的风险。
     *
     * @param command 登录命令，包含身份类型、原始 principal、密码以及客户端上下文。
     * @return 新创建会话对应的 access token 与 refresh token 对。
     * @throws AuthenticationFailed 当身份不存在、被禁用或密码校验失败时抛出。
     */
    suspend fun login(command: LoginCommand): AuthenticatedTokenPair {
        val now = clockProvider.currentInstant()
        val normalizedPrincipal = normalizePrincipal(command.identityType, command.principal)
        val user = authenticationRepository.findUserByIdentity(command.identityType, normalizedPrincipal)
        if (user == null || !user.enabled || user.passwordHash.isNullOrBlank() || !passwordHasher.matches(
                command.password,
                user.passwordHash
            )
        ) {
            authenticationRepository.recordAuthenticationLog(
                AuthenticationLogEntry(
                    clientId = command.clientType.name,
                    errorMessage = "Invalid credentials.",
                    grantType = "password",
                    identityType = command.identityType,
                    ip = command.ip,
                    occurredAt = now,
                    principal = command.principal.trim(),
                    resultCode = "INVALID_CREDENTIALS",
                    sessionId = null,
                    success = false,
                    userAgent = command.userAgent,
                    userId = user?.id,
                    username = user?.username,
                ),
            )
            throw AuthenticationFailed()
        }

        // access token 仅承载最小身份信息；真正的 refresh token 明文只在本次响应中返回一次，落库时只保存 hash。
        val refreshToken = opaqueTokenService.generateOpaqueToken()
        val refreshTokenExpiresAt = now.plus(refreshTokenTtl)
        val session =
            authenticationRepository.createSession(
                user = user,
                request =
                    SessionCreationRequest(
                        sessionKey = opaqueTokenService.generateSessionKey(),
                        clientType = command.clientType,
                        deviceName = command.deviceName,
                        deviceFingerprint = command.deviceFingerprint,
                        userAgent = command.userAgent,
                        ip = command.ip,
                        issuedAt = now,
                        refreshTokenHash = opaqueTokenService.hashToken(refreshToken),
                        refreshTokenExpiresAt = refreshTokenExpiresAt,
                        maxActiveSessions = maxActiveSessionsFor(command.clientType),
                    ),
            )
        val accessToken = accessTokenIssuer.issue(user = user, sessionId = session.sessionKey, issuedAt = now)

        authenticationRepository.recordAuthenticationLog(
            AuthenticationLogEntry(
                clientId = command.clientType.name,
                errorMessage = null,
                grantType = "password",
                identityType = command.identityType,
                ip = command.ip,
                occurredAt = now,
                principal = command.principal.trim(),
                resultCode = "LOGIN_SUCCESS",
                sessionId = session.sessionKey,
                success = true,
                userAgent = command.userAgent,
                userId = user.id,
                username = user.username,
            ),
        )

        return AuthenticatedTokenPair(
            accessToken = accessToken.token,
            accessTokenExpiresAt = accessToken.expiresAt,
            refreshToken = refreshToken,
            refreshTokenExpiresAt = refreshTokenExpiresAt,
            sessionId = session.sessionKey,
        )
    }

    /**
     * 使用 refresh token 轮换令牌对。
     *
     * 设计思路是把 rotation 事务细节全部下沉到仓储层，
     * 确保旧 token 失效、新 token 创建和会话时间推进在同一事务里完成。
     *
     * @param refreshToken 客户端持有的 refresh token 明文。
     * @return 新的 access token 与 refresh token 对。
     * @throws AuthenticationFailed 当 refresh token 无效、过期或不可继续使用时抛出。
     */
    suspend fun refresh(refreshToken: String): AuthenticatedTokenPair {
        val now = clockProvider.currentInstant()
        val newRefreshToken = opaqueTokenService.generateOpaqueToken()
        // rotation 细节下沉到 repository，在同一事务里完成旧 token 失效、新 token 创建与 session 更新时间推进。
        val refreshResult =
            authenticationRepository.rotateRefreshToken(
                request =
                    RefreshTokenRotationRequest(
                        currentTokenHash = opaqueTokenService.hashToken(refreshToken),
                        newTokenHash = opaqueTokenService.hashToken(newRefreshToken),
                        rotatedAt = now,
                        newRefreshTokenExpiresAt = now.plus(refreshTokenTtl),
                    ),
            ) ?: throw AuthenticationFailed("Invalid refresh token.")

        val accessToken = accessTokenIssuer.issue(refreshResult.user, refreshResult.session.sessionKey, now)
        authenticationRepository.recordAuthenticationLog(
            AuthenticationLogEntry(
                clientId = refreshResult.session.clientType.name,
                errorMessage = null,
                grantType = "refresh_token",
                identityType = null,
                ip = refreshResult.session.ip,
                occurredAt = now,
                principal = refreshResult.user.username,
                resultCode = "REFRESH_SUCCESS",
                sessionId = refreshResult.session.sessionKey,
                success = true,
                userAgent = refreshResult.session.userAgent,
                userId = refreshResult.user.id,
                username = refreshResult.user.username,
            ),
        )

        return AuthenticatedTokenPair(
            accessToken = accessToken.token,
            accessTokenExpiresAt = accessToken.expiresAt,
            refreshToken = newRefreshToken,
            refreshTokenExpiresAt = refreshResult.session.expiresAt,
            sessionId = refreshResult.session.sessionKey,
        )
    }

    /**
     * 撤销当前会话。
     *
     * @param principal 当前已认证会话主体。
     */
    suspend fun logout(principal: AuthenticatedSessionPrincipal) {
        val now = clockProvider.currentInstant()
        authenticationRepository.revokeSession(principal.userId, principal.sessionId, SessionRevokedReason.LOGOUT, now)
        authenticationRepository.recordAuthenticationLog(
            AuthenticationLogEntry(
                clientId = null,
                errorMessage = null,
                grantType = null,
                identityType = null,
                ip = null,
                occurredAt = now,
                principal = null,
                resultCode = "LOGOUT_SUCCESS",
                sessionId = principal.sessionId,
                success = true,
                userAgent = null,
                userId = principal.userId,
                username = null,
            ),
        )
    }

    /**
     * 撤销当前用户的全部会话。
     *
     * @param principal 当前已认证会话主体。
     */
    suspend fun logoutAll(principal: AuthenticatedSessionPrincipal) {
        val now = clockProvider.currentInstant()
        authenticationRepository.revokeAllSessions(principal.userId, SessionRevokedReason.LOGOUT_ALL, now)
        authenticationRepository.recordAuthenticationLog(
            AuthenticationLogEntry(
                clientId = null,
                errorMessage = null,
                grantType = null,
                identityType = null,
                ip = null,
                occurredAt = now,
                principal = null,
                resultCode = "LOGOUT_ALL_SUCCESS",
                sessionId = principal.sessionId,
                success = true,
                userAgent = null,
                userId = principal.userId,
                username = null,
            ),
        )
    }

    /**
     * 列出当前用户可见的会话。
     *
     * @param principal 当前已认证会话主体。
     * @return 当前用户的会话列表。
     */
    suspend fun listSessions(principal: AuthenticatedSessionPrincipal): List<UserSession> =
        authenticationRepository.listSessions(principal.userId)

    /**
     * 撤销指定会话。
     *
     * @param principal 当前已认证会话主体。
     * @param sessionId 目标会话键。
     * @throws AuthenticationSessionNotFound 当目标会话不存在或不属于当前用户时抛出。
     */
    suspend fun revokeSession(
        principal: AuthenticatedSessionPrincipal,
        sessionId: String,
    ) {
        val revoked =
            authenticationRepository.revokeSession(
                principal.userId,
                sessionId,
                SessionRevokedReason.LOGOUT,
                clockProvider.currentInstant(),
            )
        if (!revoked) {
            throw AuthenticationSessionNotFound(sessionId)
        }
    }

    /**
     * 返回当前认证用户及其授权快照。
     *
     * @param principal 当前已认证会话主体。
     * @return 当前用户的会话标识与授权快照。
     */
    suspend fun currentUser(principal: AuthenticatedSessionPrincipal): CurrentAuthenticatedUser =
        CurrentAuthenticatedUser(
            sessionId = principal.sessionId,
            snapshot = identityAccessService.getAuthorizationSnapshot(principal.userId.value),
        )

    private fun maxActiveSessionsFor(clientType: ClientType): Int =
        when (clientType) {
            ClientType.ADMIN -> maxAdminSessions
            ClientType.WEB, ClientType.APP -> Int.MAX_VALUE
        }
}