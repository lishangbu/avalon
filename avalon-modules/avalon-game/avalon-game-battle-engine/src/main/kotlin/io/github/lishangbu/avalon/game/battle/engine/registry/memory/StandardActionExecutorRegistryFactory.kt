package io.github.lishangbu.avalon.game.battle.engine.registry.memory

import io.github.lishangbu.avalon.game.battle.engine.runtime.action.StandardActionExecutors
import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId

/**
 * 标准动作执行器注册中心工厂。
 *
 * 设计意图：
 * - 把第一版标准动作执行器一次性装配到内存 registry。
 * - 作为测试和后续默认运行时装配的统一入口。
 */
object StandardActionExecutorRegistryFactory {
    fun create(): InMemoryActionExecutorRegistry {
        val entries =
            StandardActionExecutors
                .all()
                .associateBy { executor -> executor.type }
        return InMemoryActionExecutorRegistry(entries)
    }
}
