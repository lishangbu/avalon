package io.github.lishangbu.avalon.game.battle.engine.runtime.processor

import io.github.lishangbu.avalon.game.battle.engine.event.StandardHookNames
import io.github.lishangbu.avalon.game.battle.engine.loader.JsonEffectDefinitionBattleDataLoader
import io.github.lishangbu.avalon.game.battle.engine.model.BattleState
import io.github.lishangbu.avalon.game.battle.engine.model.FieldState
import io.github.lishangbu.avalon.game.battle.engine.model.UnitState
import io.github.lishangbu.avalon.game.battle.engine.mutation.BoostMutation
import io.github.lishangbu.avalon.game.battle.engine.registry.memory.StandardActionExecutorRegistryFactory
import io.github.lishangbu.avalon.game.battle.engine.registry.memory.StandardConditionInterpreterRegistryFactory
import io.github.lishangbu.avalon.game.battle.engine.runtime.DefaultHookRuleProcessor
import io.github.lishangbu.avalon.game.battle.engine.type.StandardTargetSelectorIds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 关注点：
 * - `DefaultHookRuleProcessor` 在真实 `speed-boost` 样例上是否能正确生成 residual 自强化。
 */
class DefaultHookRuleProcessorSpeedBoostFixtureTest {
    private val processor =
        DefaultHookRuleProcessor(
            conditionInterpreterRegistry = StandardConditionInterpreterRegistryFactory.create(),
            actionExecutorRegistry = StandardActionExecutorRegistryFactory.create(),
        )

    /**
     * 验证：
     * - `speed-boost` 在回合末且回合数达到阈值后，会对自身生成速度提升 mutation。
     *
     * 这条测试主要覆盖 residual hook + turn_compare + boost 的组合。
     */
    @Test
    fun shouldApplySpeedBoostWhenLoadedFixtureReachesEndOfTurn() {
        val effect =
            JsonEffectDefinitionBattleDataLoader(
                resourcePaths = listOf("data/battle/fixtures/ability/speed-boost.json"),
            ).loadEffects().single()
        val rule = requireNotNull(effect.hooks[StandardHookNames.ON_RESIDUAL]).single()

        val context =
            io.github.lishangbu.avalon.game.battle.engine.event.EventContext(
                hookName = StandardHookNames.ON_RESIDUAL,
                battle = BattleState(id = "battle-speed-1", formatId = "singles", turn = 4),
                self = UnitState(id = "p1a", currentHp = 140, maxHp = 140, abilityId = "speed-boost"),
                field = FieldState(),
                effect = effect,
            )

        val result = processor.process(rule, context)

        assertTrue(result.matched)
        assertEquals(
            listOf(BoostMutation(StandardTargetSelectorIds.SELF, mapOf("spe" to 1))),
            result.mutations,
        )
    }
}
