package io.github.lishangbu.avalon.game.battle.engine.registry.memory

import io.github.lishangbu.avalon.game.battle.engine.runtime.condition.StandardConditionInterpreters
import io.github.lishangbu.avalon.game.battle.engine.type.ConditionTypeId

/**
 * 标准条件解释器注册中心工厂。
 *
 * 设计意图：
 * - 把第一版标准条件解释器一次性装配到内存 registry。
 * - 避免测试和集成代码重复构建映射表。
 */
object StandardConditionInterpreterRegistryFactory {
    fun create(): InMemoryConditionInterpreterRegistry {
        val entries =
            StandardConditionInterpreters
                .all()
                .associateBy { interpreter -> interpreter.type }
        return InMemoryConditionInterpreterRegistry(entries)
    }
}
