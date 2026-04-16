package io.github.lishangbu.avalon.shared.infra.outbox

import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithDefault
import java.time.Duration

/**
 * Outbox 运行时配置。
 *
 * 配置读取保持最小粒度，只暴露运行时是否启用、单批大小和调度周期，
 * 避免基础设施层过早承诺更复杂的分发拓扑。
 */
@ConfigMapping(prefix = "avalon.outbox")
interface OutboxRuntimeConfig {
    /**
     * @return 当前环境是否启用 outbox runtime。
     */
    @WithDefault("false")
    fun enabled(): Boolean

    /**
     * @return 单次分发批次最多处理的消息数。
     */
    @WithDefault("25")
    fun batchSize(): Int

    /**
     * @return 调度器触发分发批次的固定周期。
     */
    @WithDefault("PT5S")
    fun dispatchInterval(): Duration
}