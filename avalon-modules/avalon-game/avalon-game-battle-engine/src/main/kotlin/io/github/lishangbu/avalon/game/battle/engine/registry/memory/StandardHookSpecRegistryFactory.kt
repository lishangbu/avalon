package io.github.lishangbu.avalon.game.battle.engine.registry.memory

import io.github.lishangbu.avalon.game.battle.engine.event.StandardHookSpecs

/**
 * 标准 HookSpec 注册中心工厂。
 *
 * 设计意图：
 * - 把第一版标准 HookSpec 一次性装配到内存 registry。
 */
object StandardHookSpecRegistryFactory {
    fun create(): InMemoryHookSpecRegistry = InMemoryHookSpecRegistry(StandardHookSpecs.all())
}
