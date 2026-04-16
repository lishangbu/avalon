package io.github.lishangbu.avalon.shared.infra.outbox

/**
 * Outbox 消息发布器契约。
 *
 * 该接口抽象“把已认领消息投递到外部通道”的动作，
 * 以便不同阶段可以接入日志、Event Bus 或真正的消息中间件实现。
 */
interface OutboxMessagePublisher {
    /**
     * 发布单条 outbox 消息。
     *
     * @param message 已完成认领的 outbox 消息。
     */
    suspend fun publish(message: StoredOutboxMessage)
}