package io.github.lishangbu.avalon.shared.kernel.domain

import java.time.Instant

/**
 * 所有跨聚合、跨上下文传播的业务事实都应实现此接口。
 *
 * 这里刻意只保留最小事件基线，不引入总线、发布器或序列化细节，
 * 以避免公共领域层被特定基础设施语义污染。各上下文可以在自己的
 * `application` 或 `infrastructure` 层决定事件如何收集、持久化和分发。
 *
 * @property occurredAt 业务事实被确认发生的时间点，而不是消息真正被投递的时间点。
 */
interface DomainEvent {
    val occurredAt: Instant
}