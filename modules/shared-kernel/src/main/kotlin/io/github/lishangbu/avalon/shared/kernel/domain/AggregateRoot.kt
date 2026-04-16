package io.github.lishangbu.avalon.shared.kernel.domain

/**
 * 聚合根基类只提供最小身份语义。
 *
 * 这里不预置版本控制、事件列表、持久化注解或仓储行为，
 * 因为这些都可能随着上下文的事务策略和技术实现不同而不同。
 * 各上下文应在自己的领域模型中补足一致性规则和行为。
 *
 * @param ID 聚合根标识类型。
 * @property id 聚合根在所属上下文中的稳定身份标识。
 */
abstract class AggregateRoot<ID>(
    open val id: ID,
)