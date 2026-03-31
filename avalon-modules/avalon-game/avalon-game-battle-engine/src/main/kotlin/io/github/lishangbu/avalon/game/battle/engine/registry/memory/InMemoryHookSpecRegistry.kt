package io.github.lishangbu.avalon.game.battle.engine.registry.memory

import io.github.lishangbu.avalon.game.battle.engine.event.HookSpec
import io.github.lishangbu.avalon.game.battle.engine.registry.HookSpecRegistry
import io.github.lishangbu.avalon.game.battle.engine.type.HookName

/**
 * 基于内存 Map 的 HookSpecRegistry 实现。
 *
 * 设计意图：
 * - 为第一版基础设施提供最简单的注册中心实现。
 * - 适用于测试、原型和本地装配。
 *
 * @property entries 以 HookName 为键的 HookSpec 映射。
 */
class InMemoryHookSpecRegistry(
    private val entries: Map<HookName, HookSpec>,
) : HookSpecRegistry {
    override fun get(name: HookName): HookSpec =
        entries[name]
            ?: error("No hook spec registered for '${name.value}'.")

    override fun contains(name: HookName): Boolean = entries.containsKey(name)
}
