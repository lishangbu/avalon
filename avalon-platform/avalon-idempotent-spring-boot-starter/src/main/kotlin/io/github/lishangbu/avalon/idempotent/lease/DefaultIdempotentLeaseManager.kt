package io.github.lishangbu.avalon.idempotent.lease

import io.github.lishangbu.avalon.idempotent.properties.IdempotentProperties
import io.github.lishangbu.avalon.idempotent.store.IdempotentStore
import java.time.Duration
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Default background lease renewer backed by a scheduled executor.
 */
class DefaultIdempotentLeaseManager(
    private val properties: IdempotentProperties,
    private val idempotentStore: IdempotentStore,
    private val scheduledExecutorService: ScheduledExecutorService,
) : IdempotentLeaseManager {
    override fun start(
        key: String,
        token: String,
    ): IdempotentLeaseManager.LeaseHandle {
        val interval = resolveRenewInterval(properties.processingTtl, properties.renewInterval)
        val future =
            scheduledExecutorService.scheduleAtFixedRate(
                {
                    val renewed =
                        runCatching {
                            idempotentStore.renew(
                                key = key,
                                token = token,
                                processingTtl = properties.processingTtl,
                            )
                        }.getOrDefault(false)
                    if (!renewed) {
                        throw StopRenewalSignal
                    }
                },
                interval.toMillis(),
                interval.toMillis(),
                TimeUnit.MILLISECONDS,
            )
        return IdempotentLeaseManager.LeaseHandle {
            future.cancel(false)
        }
    }

    private fun resolveRenewInterval(
        processingTtl: Duration,
        configuredRenewInterval: Duration?,
    ): Duration {
        val interval = configuredRenewInterval ?: processingTtl.dividedBy(3)
        return if (interval < MIN_RENEW_INTERVAL) MIN_RENEW_INTERVAL else interval
    }

    private companion object {
        val MIN_RENEW_INTERVAL: Duration = Duration.ofMillis(100)

        val StopRenewalSignal: RuntimeException =
            object : RuntimeException() {
                override fun fillInStackTrace(): Throwable = this
            }
    }
}
