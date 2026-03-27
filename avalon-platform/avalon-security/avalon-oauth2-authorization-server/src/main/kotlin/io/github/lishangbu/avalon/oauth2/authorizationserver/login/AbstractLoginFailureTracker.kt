package io.github.lishangbu.avalon.oauth2.authorizationserver.login

import io.github.lishangbu.avalon.oauth2.common.properties.Oauth2Properties
import java.time.Clock
import java.time.Duration
import java.time.Instant

data class LoginFailureState(
    val failures: Int,
    val lockUntil: Instant? = null,
)

/**
 * Shared login failure tracker configuration and state transition helpers.
 */
abstract class AbstractLoginFailureTracker(
    properties: Oauth2Properties?,
    private val clock: Clock = Clock.systemUTC(),
) : LoginFailureTracker {
    protected val maxFailures: Int = (properties?.maxLoginFailures ?: 0).coerceAtLeast(0)
    protected val lockDuration: Duration? =
        properties
            ?.getLoginLockDuration()
            ?.takeIf { !it.isZero && !it.isNegative }

    override fun isEnabled(): Boolean = maxFailures > 0 && lockDuration != null

    protected fun normalize(username: String?): String? = username?.trim()?.takeIf { it.isNotEmpty() }

    protected fun now(): Instant = Instant.now(clock)

    protected fun getRemainingLock(
        state: LoginFailureState?,
        now: Instant = now(),
    ): Duration? {
        val lockUntil = state?.lockUntil ?: return null
        if (!lockUntil.isAfter(now)) {
            return null
        }
        return Duration.between(now, lockUntil)
    }

    protected fun nextState(
        current: LoginFailureState?,
        now: Instant = now(),
    ): LoginFailureState {
        if (getRemainingLock(current, now) != null) {
            return checkNotNull(current)
        }

        val nextFailures =
            if (current?.lockUntil != null) {
                1
            } else {
                (current?.failures ?: 0) + 1
            }

        val effectiveLockDuration =
            checkNotNull(lockDuration) {
                "Login failure tracking is disabled."
            }

        if (nextFailures >= maxFailures) {
            return LoginFailureState(
                failures = 0,
                lockUntil = now.plus(effectiveLockDuration),
            )
        }
        return LoginFailureState(failures = nextFailures)
    }
}
