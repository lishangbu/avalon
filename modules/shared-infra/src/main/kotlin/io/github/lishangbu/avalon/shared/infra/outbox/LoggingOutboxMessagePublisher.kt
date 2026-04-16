package io.github.lishangbu.avalon.shared.infra.outbox

import jakarta.enterprise.context.ApplicationScoped
import org.jboss.logging.Logger

/**
 * 默认的 outbox 发布器。
 *
 * 在尚未接入真正的消息中间件之前，这个实现先把已认领的 outbox 消息写入日志，
 * 便于联调时确认 dispatcher、认领和状态流转都已经跑通。
 */
@ApplicationScoped
class LoggingOutboxMessagePublisher : OutboxMessagePublisher {
    override suspend fun publish(message: StoredOutboxMessage) {
        LOGGER.infof(
            "Publishing outbox message id=%d eventId=%s ownerContext=%s aggregate=%s/%s eventType=%s retryCount=%d",
            message.id,
            message.eventId,
            message.ownerContext,
            message.aggregateType,
            message.aggregateId,
            message.eventType,
            message.retryCount,
        )
        LOGGER.debugf("Outbox payload=%s headers=%s traceId=%s", message.payload, message.headers, message.traceId)
    }

    companion object {
        private val LOGGER: Logger = Logger.getLogger(LoggingOutboxMessagePublisher::class.java)
    }
}