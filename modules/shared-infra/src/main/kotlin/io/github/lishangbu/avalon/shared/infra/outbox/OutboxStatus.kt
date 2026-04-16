package io.github.lishangbu.avalon.shared.infra.outbox

/**
 * Outbox 消息状态。
 */
enum class OutboxStatus {
    /** 等待首次分发或下一次重试。 */
    PENDING,

    /** 已被某个 dispatcher 认领，正在分发中。 */
    DISPATCHING,

    /** 已成功发布到目标通道。 */
    PUBLISHED,

    /** 最近一次分发失败，等待下次重试。 */
    FAILED,
}