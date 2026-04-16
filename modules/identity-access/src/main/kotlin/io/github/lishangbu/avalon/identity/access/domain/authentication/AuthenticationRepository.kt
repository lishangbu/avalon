package io.github.lishangbu.avalon.identity.access.domain.authentication

import io.github.lishangbu.avalon.identity.access.domain.iam.UserId
import java.time.Instant

/**
 * 本地认证、会话和 refresh token 生命周期的仓储契约。
 *
 * 这里的实现不仅负责数据访问，还要在事务中落实认证安全语义，
 * 例如并发会话限制、refresh token rotation、reuse detection 和级联撤销。
 */
interface AuthenticationRepository {
    /**
     * 按登录标识类型查询认证用户。
     *
     * @param identityType 登录标识类型，例如用户名、邮箱或手机号。
     * @param normalizedPrincipal 归一化后的登录标识，调用方已负责去空格和统一格式。
     * @return 可用于认证的用户快照；如果不存在则返回 `null`。
     */
    suspend fun findUserByIdentity(
        identityType: IdentityType,
        normalizedPrincipal: String,
    ): AuthenticationUser?

    /**
     * 创建用户会话并落库首个 refresh token。
     *
     * 实现应在同一事务内完成会话写入、refresh token 持久化，以及必要的并发会话淘汰策略。
     *
     * @param user 已通过身份校验的认证用户。
     * @param request 会话创建请求，包含客户端类型、设备信息、refresh token hash 和会话上限。
     * @return 已持久化的用户会话。
     */
    suspend fun createSession(
        user: AuthenticationUser,
        request: SessionCreationRequest,
    ): UserSession

    /**
     * 轮换 refresh token。
     *
     * 实现应在同一事务内完成旧 token 失效、新 token 创建和会话时间推进；
     * 如果 token 非法、过期或触发 reuse detection，可返回 `null` 或由实现抛出领域异常。
     *
     * @param request refresh token rotation 所需的当前 token hash、新 token hash 和时间点信息。
     * @return 轮换后的用户与会话结果；如果当前 refresh token 无效则返回 `null`。
     */
    suspend fun rotateRefreshToken(request: RefreshTokenRotationRequest): RefreshTokenRotationResult?

    /**
     * 撤销单个会话。
     *
     * @param userId 会话所属用户标识。
     * @param sessionId 要撤销的会话键。
     * @param reason 撤销原因，用于审计与安全语义区分。
     * @param occurredAt 撤销动作发生时间。
     * @return 若命中并成功撤销至少一个会话则返回 `true`，否则返回 `false`。
     */
    suspend fun revokeSession(
        userId: UserId,
        sessionId: String,
        reason: SessionRevokedReason,
        occurredAt: Instant,
    ): Boolean

    /**
     * 撤销指定用户的全部会话。
     *
     * @param userId 目标用户标识。
     * @param reason 撤销原因。
     * @param occurredAt 撤销动作发生时间。
     * @return 被撤销的会话数量。
     */
    suspend fun revokeAllSessions(
        userId: UserId,
        reason: SessionRevokedReason,
        occurredAt: Instant,
    ): Int

    /**
     * 列出指定用户当前仍可见的会话。
     *
     * @param userId 用户标识。
     * @return 会话列表；若无可见会话则返回空列表。
     */
    suspend fun listSessions(userId: UserId): List<UserSession>

    /**
     * 记录一次认证审计日志。
     *
     * @param entry 审计日志条目，包含成功/失败结果、会话键、客户端类型和定位信息。
     */
    suspend fun recordAuthenticationLog(entry: AuthenticationLogEntry)
}