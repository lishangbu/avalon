package io.github.lishangbu.avalon.oauth2.authorizationserver.login

import java.time.Duration

/**
 * Tracks authentication failures and lock status for a user.
 *
 * <p>Implementations may persist state in-memory or in external storage to support distributed
 * deployments. / Returns whether tracking is effectively enabled (e.g. max failures > 0 and lock
 * duration > 0). Returns remaining lock duration for a username, or {@code null} if not locked /
 * disabled. Records a failed authentication attempt for the username. Clears tracking for a
 * successful authentication of the username.
 */
interface LoginFailureTracker {
    /** 判断是否启用状态 */
    fun isEnabled(): Boolean

    /** 获取剩余锁定时长 */
    fun getRemainingLock(username: String?): Duration?

    /** 处理失败 */
    fun onFailure(username: String?)

    /** 处理成功 */
    fun onSuccess(username: String?)
}
