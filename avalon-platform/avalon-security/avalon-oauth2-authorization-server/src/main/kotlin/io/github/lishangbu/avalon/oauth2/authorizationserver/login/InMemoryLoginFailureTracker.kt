package io.github.lishangbu.avalon.oauth2.authorizationserver.login

import io.github.lishangbu.avalon.oauth2.common.properties.Oauth2Properties
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/** 基于内存的登录失败跟踪器实现 */
class InMemoryLoginFailureTracker(
    properties: Oauth2Properties?,
) : LoginFailureTracker {
    /** 最大失败次数 */
    private val maxFailures: Int

    /** 锁定时长 */
    private val lockDuration: Duration?

    /** 尝试次数 */
    private val attempts = ConcurrentHashMap<String, Attempt>()

    init {
        val configuredMax = properties?.maxLoginFailures ?: 0
        maxFailures = configuredMax.coerceAtLeast(0)
        val configuredDuration = properties?.getLoginLockDuration()
        lockDuration =
            if (
                configuredDuration == null ||
                configuredDuration.isZero ||
                configuredDuration.isNegative
            ) {
                null
            } else {
                configuredDuration
            }
    }

    /** 判断是否启用状态 */
    override fun isEnabled(): Boolean = maxFailures > 0 && lockDuration != null

    /** 获取剩余锁定时长 */
    override fun getRemainingLock(username: String?): Duration? {
        if (!isEnabled()) {
            return null
        }
        val key = normalize(username) ?: return null
        val attempt = attempts[key] ?: return null
        val lockedUntil = attempt.lockUntil ?: return null
        val now = Instant.now()
        if (!lockedUntil.isAfter(now)) {
            attempts.remove(key, attempt)
            return null
        }
        return Duration.between(now, lockedUntil)
    }

    /** 处理失败 */
    override fun onFailure(username: String?) {
        if (!isEnabled()) {
            return
        }
        val key = normalize(username) ?: return
        val duration = lockDuration ?: return
        attempts.compute(key) { _, current ->
            val attempt = current ?: Attempt()
            val now = Instant.now()
            val lockedUntil = attempt.lockUntil
            if (lockedUntil != null) {
                if (lockedUntil.isAfter(now)) {
                    return@compute attempt
                }
                attempt.lockUntil = null
                attempt.failures = 0
            }
            attempt.failures += 1
            if (attempt.failures >= maxFailures) {
                attempt.lockUntil = now.plus(duration)
                attempt.failures = 0
            }
            attempt
        }
    }

    /** 处理成功 */
    override fun onSuccess(username: String?) {
        val key = normalize(username) ?: return
        attempts.remove(key)
    }

    /** 规范化用户名 */
    private fun normalize(value: String?): String? {
        val trimmed = value?.trim()
        return if (trimmed.isNullOrEmpty()) null else trimmed
    }

    private class Attempt {
        /** 失败次数 */
        var failures: Int = 0

        /** 锁定截止时间 */
        var lockUntil: Instant? = null
    }
}
