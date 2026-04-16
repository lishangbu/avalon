package io.github.lishangbu.avalon.shared.infra.outbox

import java.time.Instant
import java.util.*
import java.util.UUID

/**
 * 已持久化的 outbox 消息快照。
 *
 * 该模型面向 dispatcher 和 publisher，保留跨上下文发布所需的最小传输信息，
 * 不直接暴露任何具体业务聚合实现。
 *
 * @property id 消息主键。
 * @property eventId 业务事件全局标识。
 * @property ownerContext 拥有该消息的上下文名称。
 * @property aggregateType 产生事件的聚合类型。
 * @property aggregateId 产生事件的聚合标识。
 * @property eventType 事件类型名称。
 * @property payload 已序列化的事件载荷。
 * @property headers 已序列化的头信息。
 * @property status 当前 outbox 状态。
 * @property retryCount 已发生的重试次数。
 * @property availableAt 下一次允许被认领的时间。
 * @property occurredAt 业务事件发生时间。
 * @property claimToken 当前分发批次的认领令牌；未被认领时可为空。
 * @property claimedAt 当前分发批次的认领时间；未被认领时可为空。
 * @property traceId 链路追踪标识，可为空。
 */
data class StoredOutboxMessage(
    val id: UUID,
    val eventId: UUID,
    val ownerContext: String,
    val aggregateType: String,
    val aggregateId: String,
    val eventType: String,
    val payload: String,
    val headers: String,
    val status: OutboxStatus,
    val retryCount: Int,
    val availableAt: Instant,
    val occurredAt: Instant,
    val claimToken: UUID?,
    val claimedAt: Instant?,
    val traceId: String?,
)

