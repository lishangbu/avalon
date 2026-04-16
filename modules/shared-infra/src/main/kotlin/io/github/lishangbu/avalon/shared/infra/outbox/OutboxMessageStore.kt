package io.github.lishangbu.avalon.shared.infra.outbox

import java.time.Instant
import java.util.*
import java.util.UUID

/**
 * Outbox 消息存储契约。
 *
 * 该接口屏蔽具体数据库访问细节，只暴露 dispatcher 所需的最小状态流转操作，
 * 避免分发器直接耦合某个上下文的 SQL 实现。
 */
interface OutboxMessageStore {
    /**
     * 认领一批当前可分发的 outbox 消息。
     *
     * @param limit 本次批次最多认领的消息数。
     * @param referenceTime 用于判断消息是否到达可分发时间的参考时刻。
     * @return 已被当前 dispatcher 认领的消息列表；实现可以顺带回收超时的 `DISPATCHING` 消息。
     */
    suspend fun claimReadyMessages(
        limit: Int,
        referenceTime: Instant,
    ): List<StoredOutboxMessage>

    /**
     * 把消息标记为已发布。
     *
     * @param messageId 目标消息主键。
     * @param claimToken 当前分发批次的认领令牌。
     * @param publishedAt 发布成功时间。
     */
    suspend fun markPublished(
        messageId: UUID,
        claimToken: UUID,
        publishedAt: Instant,
    )

    /**
     * 把消息标记为发布失败并安排下次重试。
     *
     * @param messageId 目标消息主键。
     * @param claimToken 当前分发批次的认领令牌。
     * @param nextAvailableAt 下次允许重新认领的时间。
     * @param lastError 最近一次发布失败的错误摘要。
     */
    suspend fun markFailed(
        messageId: UUID,
        claimToken: UUID,
        nextAvailableAt: Instant,
        lastError: String,
    )
}

