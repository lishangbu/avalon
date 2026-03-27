package io.github.lishangbu.avalon.idempotent.store

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import tools.jackson.databind.json.JsonMapper
import java.time.Duration

/**
 * Redis implementation of [IdempotentStore].
 */
class RedisIdempotentStore(
    private val stringRedisTemplate: StringRedisTemplate,
    private val jsonMapper: JsonMapper,
) : IdempotentStore {
    private val completeScript =
        DefaultRedisScript<Long>().apply {
            setScriptText(COMPLETE_SCRIPT)
            setResultType(Long::class.java)
        }
    private val releaseScript =
        DefaultRedisScript<Long>().apply {
            setScriptText(RELEASE_SCRIPT)
            setResultType(Long::class.java)
        }
    private val renewScript =
        DefaultRedisScript<Long>().apply {
            setScriptText(RENEW_SCRIPT)
            setResultType(Long::class.java)
        }

    override fun acquire(
        key: String,
        token: String,
        processingTtl: Duration,
    ): IdempotentStore.AcquireResult {
        val pendingValue = serialize(StoredValue.processing(token))
        if (stringRedisTemplate.opsForValue().setIfAbsent(key, pendingValue, processingTtl) == true) {
            return IdempotentStore.AcquireResult.Acquired
        }

        repeat(2) {
            val existingValue = stringRedisTemplate.opsForValue().get(key)
            if (existingValue != null) {
                return when (deserialize(existingValue).status) {
                    StoredStatus.PROCESSING -> {
                        IdempotentStore.AcquireResult.Processing
                    }

                    StoredStatus.SUCCEEDED -> {
                        IdempotentStore.AcquireResult.Completed(
                            cachedValue = deserialize(existingValue).cachedValue,
                        )
                    }
                }
            }
            if (stringRedisTemplate.opsForValue().setIfAbsent(key, pendingValue, processingTtl) == true) {
                return IdempotentStore.AcquireResult.Acquired
            }
        }

        return IdempotentStore.AcquireResult.Processing
    }

    override fun complete(
        key: String,
        token: String,
        cachedValue: String?,
        ttl: Duration,
    ): Boolean =
        stringRedisTemplate.execute(
            completeScript,
            listOf(key),
            token,
            serialize(StoredValue.succeeded(token, cachedValue)),
            ttl.toMillis().toString(),
        ) == 1L

    override fun release(
        key: String,
        token: String,
    ): Boolean = stringRedisTemplate.execute(releaseScript, listOf(key), token) == 1L

    override fun renew(
        key: String,
        token: String,
        processingTtl: Duration,
    ): Boolean =
        stringRedisTemplate.execute(
            renewScript,
            listOf(key),
            token,
            processingTtl.toMillis().toString(),
        ) == 1L

    private fun serialize(value: StoredValue): String = jsonMapper.writeValueAsString(value)

    private fun deserialize(value: String): StoredValue = jsonMapper.readValue(value, StoredValue::class.java)

    private data class StoredValue(
        val status: StoredStatus,
        val token: String,
        val cachedValue: String? = null,
    ) {
        companion object {
            fun processing(token: String): StoredValue = StoredValue(status = StoredStatus.PROCESSING, token = token)

            fun succeeded(
                token: String,
                cachedValue: String?,
            ): StoredValue = StoredValue(status = StoredStatus.SUCCEEDED, token = token, cachedValue = cachedValue)
        }
    }

    private enum class StoredStatus {
        PROCESSING,
        SUCCEEDED,
    }

    companion object {
        private const val COMPLETE_SCRIPT: String =
            """
            local current = redis.call('GET', KEYS[1])
            if not current then
              return 0
            end
            local decoded = cjson.decode(current)
            if decoded['status'] ~= 'PROCESSING' or decoded['token'] ~= ARGV[1] then
              return 0
            end
            redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[3])
            return 1
            """

        private const val RELEASE_SCRIPT: String =
            """
            local current = redis.call('GET', KEYS[1])
            if not current then
              return 0
            end
            local decoded = cjson.decode(current)
            if decoded['token'] ~= ARGV[1] then
              return 0
            end
            redis.call('DEL', KEYS[1])
            return 1
            """

        private const val RENEW_SCRIPT: String =
            """
            local current = redis.call('GET', KEYS[1])
            if not current then
              return 0
            end
            local decoded = cjson.decode(current)
            if decoded['status'] ~= 'PROCESSING' or decoded['token'] ~= ARGV[1] then
              return 0
            end
            redis.call('PEXPIRE', KEYS[1], ARGV[2])
            return 1
            """
    }
}
