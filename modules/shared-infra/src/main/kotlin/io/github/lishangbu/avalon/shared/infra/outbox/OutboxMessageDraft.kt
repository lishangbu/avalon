package io.github.lishangbu.avalon.shared.infra.outbox

import java.time.Instant
import java.util.*
import java.util.UUID

/**
 * 待写入 outbox 的消息草稿。
 *
 * 该草稿只保留发布链路所需的最小信息：事件标识、归属上下文、聚合坐标、
 * 事件类型、载荷与头信息。具体业务上下文应当先完成本地事务中的业务写入，
 * 再把需要跨上下文传播的事实追加到 outbox。
 *
 * @property eventId 业务事件全局标识，建议在调用方侧稳定生成。
 * @property ownerContext 拥有该消息的 bounded context 名称。
 * @property aggregateType 产生该事件的聚合类型。
 * @property aggregateId 产生该事件的聚合标识。
 * @property eventType 事件类型名称。
 * @property payload 事件载荷，建议以 JSON 字符串形式存储。
 * @property headers 事件头信息，建议以 JSON 字符串形式存储。
 * @property occurredAt 业务事件发生时间。
 * @property availableAt 首次允许被 dispatcher 认领的时间。
 * @property traceId 链路追踪标识，可为空。
 */
data class OutboxMessageDraft(
    val eventId: UUID,
    val ownerContext: String,
    val aggregateType: String,
    val aggregateId: String,
    val eventType: String,
    val payload: String,
    val headers: String = "{}",
    val occurredAt: Instant,
    val availableAt: Instant,
    val traceId: String? = null,
)