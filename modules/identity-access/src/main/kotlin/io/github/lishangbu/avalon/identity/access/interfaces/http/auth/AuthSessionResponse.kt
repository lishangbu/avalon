package io.github.lishangbu.avalon.identity.access.interfaces.http.auth

import io.github.lishangbu.avalon.identity.access.domain.authentication.ClientType
import io.github.lishangbu.avalon.identity.access.domain.authentication.UserSession

/**
 * 认证会话响应。
 *
 * 该响应面向会话管理界面，展示单个会话的客户端来源、状态和时间轴。
 *
 * @property sessionId 会话稳定标识。
 * @property clientType 会话所属客户端类型。
 * @property deviceName 会话创建时记录的设备名称。
 * @property current 是否为当前请求所在会话。
 * @property status 会话状态字符串。
 * @property lastRefreshedAt 最近一次刷新 refresh token 的时间。
 * @property expiresAt 会话整体过期时间。
 */
data class AuthSessionResponse(
    val sessionId: String,
    val clientType: ClientType,
    val deviceName: String?,
    val current: Boolean,
    val status: String,
    val lastRefreshedAt: String,
    val expiresAt: String,
)

/**
 * 把会话领域对象转换为会话管理响应。
 *
 * @param currentSessionId 当前访问链路所属的会话标识，用于计算 [AuthSessionResponse.current]。
 * @return 可直接暴露给会话管理接口的响应对象。
 */
fun UserSession.toResponse(currentSessionId: String): AuthSessionResponse =
    AuthSessionResponse(
        sessionId = sessionKey,
        clientType = clientType,
        deviceName = deviceName,
        current = sessionKey == currentSessionId,
        status = status.name,
        lastRefreshedAt = lastRefreshedAt.toString(),
        expiresAt = expiresAt.toString(),
    )

/**
 * 把会话集合转换为会话管理响应列表。
 *
 * @param currentSessionId 当前访问链路所属的会话标识，用于计算每个响应的 [AuthSessionResponse.current]。
 * @return 已按单个会话映射规则展开的响应列表。
 */
internal fun Iterable<UserSession>.toResponses(currentSessionId: String): List<AuthSessionResponse> =
    map { it.toResponse(currentSessionId) }