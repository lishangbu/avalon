package io.github.lishangbu.avalon.shared.infra

/**
 * shared-infra 只承载多个上下文都需要依赖的技术型基础设施能力。
 *
 * 当前主要放置跨上下文通用但不属于公共领域语言的运行时组件，
 * 例如 outbox runtime 基线、outbox writer/store/publisher、统一时间源实现、
 * HTTP/SQL 适配与 PostgreSQL 分页语法辅助等。
 * 这里不应该沉淀任何特定上下文的业务规则、聚合模型或接口层 DTO。
 */
object SharedInfraModule