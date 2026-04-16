package io.github.lishangbu.avalon.shared.infra.outbox

import java.time.Instant

/**
 * Outbox 分发执行器契约。
 *
 * 运行时调度器只负责按计划触发分发；真正如何认领消息、发布消息和更新状态，
 * 由实现类根据当前上下文的 outbox 运行时方案决定。
 */
interface OutboxDispatchExecutor {
    /**
     * 分发当前时刻可被处理的 outbox 消息。
     *
     * @param referenceTime 本次分发批次使用的统一参考时间。
     */
    suspend fun dispatchReadyMessages(referenceTime: Instant)
}