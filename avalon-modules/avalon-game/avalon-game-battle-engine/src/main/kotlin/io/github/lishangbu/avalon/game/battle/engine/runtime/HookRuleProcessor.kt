package io.github.lishangbu.avalon.game.battle.engine.runtime

import io.github.lishangbu.avalon.game.battle.engine.dsl.HookRule
import io.github.lishangbu.avalon.game.battle.engine.event.EventContext

/**
 * Hook 规则处理器接口。
 */
interface HookRuleProcessor {
    fun process(
        rule: HookRule,
        context: EventContext,
    ): HookRuleResult
}
