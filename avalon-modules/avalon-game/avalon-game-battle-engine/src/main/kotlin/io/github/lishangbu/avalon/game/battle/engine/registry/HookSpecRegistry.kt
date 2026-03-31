package io.github.lishangbu.avalon.game.battle.engine.registry

import io.github.lishangbu.avalon.game.battle.engine.event.HookSpec
import io.github.lishangbu.avalon.game.battle.engine.type.HookName

/**
 * HookSpec 注册中心。
 *
 * 设计意图：
 * - 统一管理 Hook 元信息。
 * - 避免 Hook 语义散落在业务代码中。
 */
interface HookSpecRegistry {
    /**
     * 获取指定 Hook 的元信息。
     */
    fun get(name: HookName): HookSpec

    /**
     * 判断指定 Hook 是否已经注册。
     */
    fun contains(name: HookName): Boolean
}
