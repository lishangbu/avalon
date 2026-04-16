package io.github.lishangbu.avalon.shared.kernel.domain

/**
 * 统一的业务规则异常基类。
 *
 * 各上下文应在自己的领域内派生更具体的异常类型，再由接口层做异常映射；
 * 不要直接把底层数据库或框架异常暴露为领域失败。
 *
 * @param message 领域规则被违反时面向开发者和调用链的说明信息。
 */
open class DomainRuleViolation(
    message: String,
) : RuntimeException(message)