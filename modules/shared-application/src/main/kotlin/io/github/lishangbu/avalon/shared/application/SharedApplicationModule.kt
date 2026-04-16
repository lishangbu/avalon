package io.github.lishangbu.avalon.shared.application

/**
 * shared-application 承载跨上下文复用的应用层公共契约。
 *
 * 这里适合放稳定、业务中性、且不绑定 HTTP、SQL、Quarkus runtime 的用例层类型，
 * 例如分页查询模型、时间源契约等。它不负责承载领域内核概念，也不放具体技术适配：
 * 领域公共语言继续放在 `shared-kernel`，HTTP/SQL/outbox 等运行时实现继续放在 `shared-infra`。
 */
object SharedApplicationModule