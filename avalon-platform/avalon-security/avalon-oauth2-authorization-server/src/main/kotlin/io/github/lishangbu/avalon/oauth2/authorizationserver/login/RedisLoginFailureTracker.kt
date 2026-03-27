package io.github.lishangbu.avalon.oauth2.authorizationserver.login

import io.github.lishangbu.avalon.oauth2.common.properties.Oauth2Properties
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import java.time.Clock
import java.time.Duration

/**
 * Redis-backed login failure tracker.
 */
class RedisLoginFailureTracker(
    properties: Oauth2Properties?,
    private val stringRedisTemplate: StringRedisTemplate,
    clock: Clock = Clock.systemUTC(),
) : AbstractLoginFailureTracker(properties, clock) {
    private val keyPrefix =
        properties
            ?.loginFailureTrackerKeyPrefix
            ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_KEY_PREFIX

    private val failureScript =
        DefaultRedisScript<Long>().apply {
            setScriptText(FAILURE_SCRIPT)
            setResultType(Long::class.java)
        }

    private val remainingLockScript =
        DefaultRedisScript<Long>().apply {
            setScriptText(REMAINING_LOCK_SCRIPT)
            setResultType(Long::class.java)
        }

    override fun getRemainingLock(username: String?): Duration? {
        if (!isEnabled()) {
            return null
        }
        val key = normalize(username)?.let(::buildStorageKey) ?: return null
        val remaining =
            stringRedisTemplate.execute(
                remainingLockScript,
                listOf(key),
                now().toEpochMilli().toString(),
            ) ?: 0L

        return remaining.takeIf { it > 0 }?.let(Duration::ofMillis)
    }

    override fun onFailure(username: String?) {
        if (!isEnabled()) {
            return
        }
        val key = normalize(username)?.let(::buildStorageKey) ?: return
        stringRedisTemplate.execute(
            failureScript,
            listOf(key),
            now().toEpochMilli().toString(),
            maxFailures.toString(),
            checkNotNull(lockDuration).toMillis().toString(),
        )
    }

    override fun onSuccess(username: String?) {
        val key = normalize(username)?.let(::buildStorageKey) ?: return
        stringRedisTemplate.delete(key)
    }

    private fun buildStorageKey(username: String): String = "$keyPrefix:$username"

    companion object {
        const val DEFAULT_KEY_PREFIX: String = "oauth2:login-failure"

        private const val FAILURE_SCRIPT: String =
            """
            local currentLockUntil = redis.call('HGET', KEYS[1], 'lockUntil')
            local nowMillis = tonumber(ARGV[1])
            local maxFailures = tonumber(ARGV[2])
            local lockDurationMillis = tonumber(ARGV[3])

            if currentLockUntil then
              local lockUntilMillis = tonumber(currentLockUntil)
              if lockUntilMillis and lockUntilMillis > nowMillis then
                return lockUntilMillis - nowMillis
              end
              redis.call('DEL', KEYS[1])
            end

            local failures = redis.call('HINCRBY', KEYS[1], 'failures', 1)
            if failures >= maxFailures then
              local lockUntilMillis = nowMillis + lockDurationMillis
              redis.call('HSET', KEYS[1], 'failures', 0, 'lockUntil', lockUntilMillis)
              redis.call('PEXPIRE', KEYS[1], lockDurationMillis)
              return lockDurationMillis
            end

            redis.call('HDEL', KEYS[1], 'lockUntil')
            redis.call('PERSIST', KEYS[1])
            return 0
            """

        private const val REMAINING_LOCK_SCRIPT: String =
            """
            local currentLockUntil = redis.call('HGET', KEYS[1], 'lockUntil')
            if not currentLockUntil then
              return 0
            end

            local remaining = tonumber(currentLockUntil) - tonumber(ARGV[1])
            if remaining <= 0 then
              redis.call('DEL', KEYS[1])
              return 0
            end
            return remaining
            """
    }
}
