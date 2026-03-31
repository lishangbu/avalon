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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 关注点：
 * - `DefaultHookRuleProcessor` 在真实 `will-o-wisp` 样例上是否能正确在非火属性目标上附加灼伤。
 */
class DefaultHookRuleProcessorWillOWispFixtureTest {
    private val processor =
        DefaultHookRuleProcessor(
            conditionInterpreterRegistry = StandardConditionInterpreterRegistryFactory.create(),
            actionExecutorRegistry = StandardActionExecutorRegistryFactory.create(),
        )

    /**
     * 验证：
     * - `will-o-wisp` 在目标不是火属性时，会施加灼伤。
     *
     * 这条测试覆盖 `not + has_type + add_status` 的组合。
     */
    @Test
    fun shouldApplyWillOWispBurnWhenLoadedFixtureTargetIsNotFireType() {
        val effect =
            JsonEffectDefinitionBattleDataLoader(
                resourcePaths = listOf("data/battle/fixtures/move/will-o-wisp.json"),
            ).loadEffects().single()
        val rule = requireNotNull(effect.hooks[StandardHookNames.ON_HIT]).single()

        val context =
            io.github.lishangbu.avalon.game.battle.engine.event.EventContext(
                hookName = StandardHookNames.ON_HIT,
                battle = BattleState(id = "battle-wow-1", formatId = "singles", turn = 2),
                self = UnitState(id = "p1a", currentHp = 120, maxHp = 120),
                target = UnitState(id = "p2a", currentHp = 100, maxHp = 100, typeIds = setOf("grass")),
                field = FieldState(),
                effect = effect,
            )

        val result = processor.process(rule, context)

        assertTrue(result.matched)
        assertEquals(
            listOf(SetStatusMutation(StandardTargetSelectorIds.TARGET, "brn")),
            result.mutations,
        )
    }
}
