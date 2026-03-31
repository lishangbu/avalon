package io.github.lishangbu.avalon.game.battle.engine.runtime.processor

import io.github.lishangbu.avalon.game.battle.engine.event.StandardHookNames
import io.github.lishangbu.avalon.game.battle.engine.loader.JsonEffectDefinitionBattleDataLoader
import io.github.lishangbu.avalon.game.battle.engine.model.BattleState
import io.github.lishangbu.avalon.game.battle.engine.model.FieldState
import io.github.lishangbu.avalon.game.battle.engine.model.UnitState
import io.github.lishangbu.avalon.game.battle.engine.mutation.ConsumeItemMutation
import io.github.lishangbu.avalon.game.battle.engine.mutation.HealMutation
import io.github.lishangbu.avalon.game.battle.engine.registry.memory.StandardActionExecutorRegistryFactory
import io.github.lishangbu.avalon.game.battle.engine.registry.memory.StandardConditionInterpreterRegistryFactory
import io.github.lishangbu.avalon.game.battle.engine.runtime.DefaultHookRuleProcessor
import io.github.lishangbu.avalon.game.battle.engine.type.StandardTargetSelectorIds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 关注点：
 * - `DefaultHookRuleProcessor` 在真实 `sitrus-berry` 样例上是否能正确执行多动作链。
 */
class DefaultHookRuleProcessorSitrusBerryFixtureTest {
    private val processor =
        DefaultHookRuleProcessor(
            conditionInterpreterRegistry = StandardConditionInterpreterRegistryFactory.create(),
            actionExecutorRegistry = StandardActionExecutorRegistryFactory.create(),
        )

    /**
     * 验证：
     * - `sitrus-berry` 会在 `all(...)` 条件满足时同时产出“消耗道具”和“回复生命”两个 mutation。
     *
     * 这个测试证明多动作顺序执行与 `has_item + hp_ratio` 组合条件是可用的。
     */
    @Test
    fun shouldConsumeAndHealWhenLoadedSitrusBerryFixtureTriggersAtHalfHp() {
        val effect =
            JsonEffectDefinitionBattleDataLoader(
                resourcePaths = listOf("data/battle/fixtures/item/sitrus-berry.json"),
            ).loadEffects().single()
        val rule = requireNotNull(effect.hooks[StandardHookNames.ON_RESIDUAL]).single()

        val context =
            io.github.lishangbu.avalon.game.battle.engine.event.EventContext(
                hookName = StandardHookNames.ON_RESIDUAL,
                battle = BattleState(id = "battle-sitrus-1", formatId = "singles", turn = 6),
                self = UnitState(id = "p1a", currentHp = 50, maxHp = 100, itemId = "sitrus-berry"),
                field = FieldState(),
                effect = effect,
            )

        val result = processor.process(rule, context)

        assertTrue(result.matched)
        assertEquals(
            listOf(
                ConsumeItemMutation(StandardTargetSelectorIds.SELF),
                HealMutation(StandardTargetSelectorIds.SELF, "max_hp_ratio", 0.25),
            ),
            result.mutations,
        )
    }
}
