package io.github.lishangbu.avalon.shared.kernel

/**
 * shared-kernel 只承载足够稳定、足够少量、并且明确属于多个上下文公共语言的核心抽象。
 *
 * 这里适合放：
 * - 领域事件最小基线
 * - 聚合根最小抽象
 * - 统一业务规则异常基类
 *
 * 这里不适合放：
 * - 任一上下文专属模型
 * - 持久化、REST、消息、缓存等基础设施细节
 * - 仅因为“以后可能复用”而提前上移的类型
 */
object SharedKernelModule