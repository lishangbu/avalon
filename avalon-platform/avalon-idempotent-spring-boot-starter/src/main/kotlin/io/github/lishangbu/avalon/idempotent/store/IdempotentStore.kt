package io.github.lishangbu.avalon.idempotent.store

import java.time.Duration

/**
 * Persists idempotent execution state.
 */
interface IdempotentStore {
    fun acquire(
        key: String,
        token: String,
        processingTtl: Duration,
    ): AcquireResult

    fun complete(
        key: String,
        token: String,
        cachedValue: String?,
        ttl: Duration,
    ): Boolean

    fun release(
        key: String,
        token: String,
    ): Boolean

    fun renew(
        key: String,
        token: String,
        processingTtl: Duration,
    ): Boolean

    sealed interface AcquireResult {
        data object Acquired : AcquireResult

        data object Processing : AcquireResult

        data class Completed(
            val cachedValue: String?,
        ) : AcquireResult
    }
}
