package io.github.lishangbu.avalon.game.battle.engine.runtime.processor

import io.github.lishangbu.avalon.game.battle.engine.event.StandardHookNames
import io.github.lishangbu.avalon.game.battle.engine.loader.JsonEffectDefinitionBattleDataLoader
import io.github.lishangbu.avalon.game.battle.engine.model.BattleState
import io.github.lishangbu.avalon.game.battle.engine.model.FieldState
import io.github.lishangbu.avalon.game.battle.engine.model.UnitState
import io.github.lishangbu.avalon.game.battle.engine.mutation.ClearBoostsMutation
import io.github.lishangbu.avalon.game.battle.engine.registry.memory.StandardActionExecutorRegistryFactory
import io.github.lishangbu.avalon.game.battle.engine.registry.memory.StandardConditionInterpreterRegistryFactory
import io.github.lishangbu.avalon.game.battle.engine.runtime.DefaultHookRuleProcessor
import io.github.lishangbu.avalon.game.battle.engine.type.StandardTargetSelectorIds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * 关注点：
 * - `DefaultHookRuleProcessor` 在真实 `haze` 样例上是否能正确生成群体清空 boosts 的 mutation。
 */
class DefaultHookRuleProcessorHazeFixtureTest {
    private val processor =
        DefaultHookRuleProcessor(
            conditionInterpreterRegistry = StandardConditionInterpreterRegistryFactory.create(),
            actionExecutorRegistry = StandardActionExecutorRegistryFactory.create(),
        )

    /**
     * 验证：
     * - `haze` 的效果会在命中后生成面向 `all` selector 的 `clear_boosts` mutation。
     *
     * 这个测试用来证明群体目标 selector 与 `clear_boosts` 动作链是通的。
     */
    @Test
    fun shouldApplyHazeClearBoostsWhenLoadingHazeFixture() {
        val effect =
            JsonEffectDefinitionBattleDataLoader(
                resourcePaths = listOf("data/battle/fixtures/move/haze.json"),
            ).loadEffects().single()
        val rule = requireNotNull(effect.hooks[StandardHookNames.ON_HIT]).single()

        val context =
            io.github.lishangbu.avalon.game.battle.engine.event.EventContext(
                hookName = StandardHookNames.ON_HIT,
                battle = BattleState(id = "battle-haze-1", formatId = "singles", turn = 4),
                self = UnitState(id = "p1a", currentHp = 120, maxHp = 120, boosts = mapOf("atk" to 2)),
                target = UnitState(id = "p2a", currentHp = 100, maxHp = 100, boosts = mapOf("def" to 3)),
                field = FieldState(),
                effect = effect,
            )

        val result = processor.process(rule, context)

        assertEquals(
            listOf(ClearBoostsMutation(StandardTargetSelectorIds.ALL)),
            result.mutations,
        )
    }
}
