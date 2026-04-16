package io.github.lishangbu.avalon.shared.infra.outbox

import io.github.lishangbu.avalon.shared.application.time.ClockProvider
import jakarta.enterprise.context.ApplicationScoped
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * 基于 SQL outbox 的分发执行器。
 *
 * 该执行器先从 `integration.outbox_event` 中认领当前可分发消息，再逐条交给
 * [OutboxMessagePublisher] 发布；发布成功后标记为 `PUBLISHED`，失败后则递增重试次数
 * 并把下次可见时间往后推移。
 */
@ApplicationScoped
class SqlOutboxDispatchExecutor(
    private val clockProvider: ClockProvider,
    private val runtimeConfig: OutboxRuntimeConfig,
    private val messageStore: OutboxMessageStore,
    private val messagePublisher: OutboxMessagePublisher,
) : OutboxDispatchExecutor {
    override suspend fun dispatchReadyMessages(referenceTime: Instant) {
        val messages = messageStore.claimReadyMessages(runtimeConfig.batchSize(), referenceTime)
        if (messages.isEmpty()) {
            return
        }

        LOGGER.debugf("Claimed %d outbox message(s) at %s.", messages.size, referenceTime)
        for (message in messages) {
            dispatchSingle(message)
        }
    }

    private suspend fun dispatchSingle(message: StoredOutboxMessage) {
        val claimToken = message.claimToken
        if (claimToken == null) {
            LOGGER.warnf("Outbox message %d does not carry a claim token, skipping.", message.id)
            return
        }

        try {
            messagePublisher.publish(message)
        } catch (exception: Throwable) {
            handlePublishFailure(message, claimToken, exception)
            return
        }

        val publishedAt = clockProvider.currentInstant()
        runCatching {
            messageStore.markPublished(message.id, claimToken, publishedAt)
        }.onFailure { exception ->
            LOGGER.errorf(
                exception,
                "Failed to mark outbox message %d as published after a successful publish.",
                message.id,
            )
        }
    }

    private suspend fun handlePublishFailure(
        message: StoredOutboxMessage,
        claimToken: java.util.UUID,
        exception: Throwable,
    ) {
        val failedAt = clockProvider.currentInstant()
        val nextAvailableAt = failedAt.plus(retryDelayFor(message.retryCount + 1))
        val lastError = summarizeException(exception)

        runCatching {
            messageStore.markFailed(message.id, claimToken, nextAvailableAt, lastError)
        }.onFailure { markException ->
            LOGGER.errorf(
                markException,
                "Failed to mark outbox message %d as failed after publish error: %s",
                message.id,
                lastError,
            )
        }

        LOGGER.errorf(
            exception,
            "Failed to publish outbox message id=%d eventId=%s eventType=%s",
            message.id,
            message.eventId,
            message.eventType,
        )
    }

    private fun retryDelayFor(attempt: Int): Duration {
        val normalizedAttempt = attempt.coerceAtLeast(1)
        val shift = (normalizedAttempt - 1).coerceAtMost(5)
        val candidate = Duration.ofSeconds(5L * (1L shl shift))
        return if (candidate > MAX_RETRY_DELAY) MAX_RETRY_DELAY else candidate
    }

    private fun summarizeException(exception: Throwable): String {
        val message = exception.message?.takeIf { it.isNotBlank() } ?: "no message"
        val summary = "${exception::class.java.simpleName}: $message"
        return summary.take(1000)
    }

    companion object {
        private val LOGGER = org.jboss.logging.Logger.getLogger(SqlOutboxDispatchExecutor::class.java)
        private val MAX_RETRY_DELAY: Duration = Duration.ofMinutes(5)
    }
}