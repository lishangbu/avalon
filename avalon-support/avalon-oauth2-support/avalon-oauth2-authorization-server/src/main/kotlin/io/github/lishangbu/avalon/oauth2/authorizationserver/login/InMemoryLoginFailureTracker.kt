package io.github.lishangbu.avalon.oauth2.authorizationserver.login

import io.github.lishangbu.avalon.oauth2.common.properties.Oauth2Properties
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/** In-memory implementation of {@link LoginFailureTracker}. / */
class InMemoryLoginFailureTracker(
    properties: Oauth2Properties?,
) : LoginFailureTracker {
    private val maxFailures: Int
    private val lockDuration: Duration?
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

    override fun isEnabled(): Boolean = maxFailures > 0 && lockDuration != null

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

    override fun onSuccess(username: String?) {
        val key = normalize(username) ?: return
        attempts.remove(key)
    }

    private fun normalize(value: String?): String? {
        val trimmed = value?.trim()
        return if (trimmed.isNullOrEmpty()) null else trimmed
    }

    private class Attempt {
        var failures: Int = 0
        var lockUntil: Instant? = null
    }
}
