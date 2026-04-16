package io.github.lishangbu.avalon.shared.infra.outbox

import io.github.lishangbu.avalon.shared.application.time.ClockProvider
import io.quarkus.scheduler.Scheduled
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Instance
import kotlinx.coroutines.runBlocking
import org.jboss.logging.Logger

/**
 * Outbox 调度入口。
 *
 * 该类只负责按配置周期触发一次分发批次，并在运行时判断 outbox 是否启用、
 * 当前是否已经注册 dispatcher 实现。真正的消息认领与发布逻辑由
 * [OutboxDispatchExecutor] 承担。
 */
@ApplicationScoped
class OutboxDispatchScheduler(
    private val clockProvider: ClockProvider,
    private val runtimeConfig: OutboxRuntimeConfig,
    private val dispatchExecutors: Instance<OutboxDispatchExecutor>,
) {
    /**
     * 触发一次 outbox 分发批次。
     *
     * 若运行时未启用 outbox，或当前环境尚未提供 [OutboxDispatchExecutor]，
     * 该方法会直接返回，不会制造空跑错误。
     */
    @Scheduled(
        identity = "avalon-outbox-dispatcher",
        every = "{avalon.outbox.dispatch-interval}",
        concurrentExecution = Scheduled.ConcurrentExecution.SKIP,
    )
    fun dispatchReadyMessages() {
        if (!runtimeConfig.enabled()) {
            return
        }

        val executor = resolveExecutor() ?: run {
            LOGGER.debug("Outbox dispatcher is enabled, but no OutboxDispatchExecutor bean is registered yet.")
            return
        }

        runBlocking {
            executor.dispatchReadyMessages(clockProvider.currentInstant())
        }
    }

    private fun resolveExecutor(): OutboxDispatchExecutor? {
        if (dispatchExecutors.isUnsatisfied) {
            return null
        }

        return dispatchExecutors.iterator().next()
    }

    companion object {
        private val LOGGER: Logger = Logger.getLogger(OutboxDispatchScheduler::class.java)
    }
}