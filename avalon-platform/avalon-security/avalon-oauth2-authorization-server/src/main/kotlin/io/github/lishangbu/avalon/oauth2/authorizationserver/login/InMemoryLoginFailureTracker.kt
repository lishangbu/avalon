package io.github.lishangbu.avalon.oauth2.authorizationserver.login

import io.github.lishangbu.avalon.oauth2.common.properties.Oauth2Properties
import java.time.Clock
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/** 基于内存的登录失败跟踪器实现 */
class InMemoryLoginFailureTracker(
    properties: Oauth2Properties?,
    clock: Clock = Clock.systemUTC(),
) : AbstractLoginFailureTracker(properties, clock) {
    /** 尝试次数 */
    private val attempts = ConcurrentHashMap<String, LoginFailureState>()

    /** 获取剩余锁定时长 */
    override fun getRemainingLock(username: String?): Duration? {
        if (!isEnabled()) {
            return null
        }
        val key = normalize(username) ?: return null
        val state = attempts[key] ?: return null
        val currentTime = now()
        val remainingLock = getRemainingLock(state, currentTime)
        if (remainingLock == null && state.lockUntil != null) {
            attempts.remove(key, state)
        }
        return remainingLock
    }

    /** 处理失败 */
    override fun onFailure(username: String?) {
        if (!isEnabled()) {
            return
        }
        val key = normalize(username) ?: return
        val currentTime = now()
        attempts.compute(key) { _, current ->
            nextState(current, currentTime)
        }
    }

    /** 处理成功 */
    override fun onSuccess(username: String?) {
        val key = normalize(username) ?: return
        attempts.remove(key)
    }
}
