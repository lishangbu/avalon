package io.github.lishangbu.avalon.game.battle.engine.runtime.processor

import io.github.lishangbu.avalon.game.battle.engine.event.StandardHookNames
import io.github.lishangbu.avalon.game.battle.engine.loader.JsonEffectDefinitionBattleDataLoader
import io.github.lishangbu.avalon.game.battle.engine.model.BattleState
import io.github.lishangbu.avalon.game.battle.engine.model.FieldState
import io.github.lishangbu.avalon.game.battle.engine.model.UnitState
import io.github.lishangbu.avalon.game.battle.engine.mutation.SetStatusMutation
import io.github.lishangbu.avalon.game.battle.engine.registry.memory.StandardActionExecutorRegistryFactory
import io.github.lishangbu.avalon.game.battle.engine.registry.memory.StandardConditionInterpreterRegistryFactory
import io.github.lishangbu.avalon.game.battle.engine.runtime.DefaultHookRuleProcessor
import io.github.lishangbu.avalon.game.battle.engine.type.StandardTargetSelectorIds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 关注点：
 * - `DefaultHookRuleProcessor` 在真实 `thunderbolt` 样例上是否能正确处理概率性麻痹附加。
 */
class DefaultHookRuleProcessorThunderboltFixtureTest {
    private val processor =
        DefaultHookRuleProcessor(
            conditionInterpreterRegistry = StandardConditionInterpreterRegistryFactory.create(),
            actionExecutorRegistry = StandardActionExecutorRegistryFactory.create(),
        )

    /**
     * 验证：
     * - `thunderbolt` 的二次效果在概率满足时，会对目标施加麻痹。
     *
     * 这是最经典的“命中后二次状态附加”基线样例。
     */
    @Test
    fun shouldApplyThunderboltParalysisWhenLoadedFixtureChanceSucceeds() {
        val effect =
            JsonEffectDefinitionBattleDataLoader(
                resourcePaths = listOf("data/battle/fixtures/move/thunderbolt.json"),
            ).loadEffects().single()
        val rule = requireNotNull(effect.hooks[StandardHookNames.ON_HIT]).single()

        val context =
            io.github.lishangbu.avalon.game.battle.engine.event.EventContext(
                hookName = StandardHookNames.ON_HIT,
                battle = BattleState(id = "battle-1", formatId = "singles", turn = 3),
                self = UnitState(id = "p1a", currentHp = 120, maxHp = 120),
                target = UnitState(id = "p2a", currentHp = 100, maxHp = 100),
                field = FieldState(),
                effect = effect,
                attributes = mapOf("chanceRoll" to 7),
            )

        val result = processor.process(rule, context)

        assertTrue(result.matched)
        assertFalse(result.cancelled)
        assertEquals(
            listOf(SetStatusMutation(StandardTargetSelectorIds.TARGET, "par")),
            result.mutations,
        )
    }
}
