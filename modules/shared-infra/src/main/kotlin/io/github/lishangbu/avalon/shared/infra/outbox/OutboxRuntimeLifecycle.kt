package io.github.lishangbu.avalon.shared.infra.outbox

import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.enterprise.inject.Instance
import org.jboss.logging.Logger

/**
 * Outbox runtime 生命周期日志入口。
 *
 * 目前主要在应用启动时输出 outbox 基线是否启用、批次大小和执行器注册情况，
 * 方便在开发与联调环境快速确认运行时配置是否生效。
 */
@ApplicationScoped
class OutboxRuntimeLifecycle(
    private val runtimeConfig: OutboxRuntimeConfig,
    private val dispatchExecutors: Instance<OutboxDispatchExecutor>,
) {
    /**
     * 在应用启动后输出当前 outbox runtime 基线信息。
     *
     * @param startupEvent Quarkus 启动事件，仅用于声明观察点。
     */
    fun onStart(@Observes startupEvent: StartupEvent) {
        LOGGER.infof(
            "Outbox runtime baseline ready (enabled=%s, batchSize=%d, executorRegistered=%s).",
            runtimeConfig.enabled(),
            runtimeConfig.batchSize(),
            !dispatchExecutors.isUnsatisfied,
        )
    }

    companion object {
        private val LOGGER: Logger = Logger.getLogger(OutboxRuntimeLifecycle::class.java)
    }
}