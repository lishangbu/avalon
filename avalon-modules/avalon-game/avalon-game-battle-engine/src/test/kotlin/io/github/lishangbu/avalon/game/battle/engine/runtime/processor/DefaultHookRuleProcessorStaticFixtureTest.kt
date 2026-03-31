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
 * - `DefaultHookRuleProcessor` 在真实 `static` 样例上是否能正确处理概率 + 目标关系组合条件。
 */
class DefaultHookRuleProcessorStaticFixtureTest {
    private val processor =
        DefaultHookRuleProcessor(
            conditionInterpreterRegistry = StandardConditionInterpreterRegistryFactory.create(),
            actionExecutorRegistry = StandardActionExecutorRegistryFactory.create(),
        )

    /**
     * 验证：
     * - `static` 这种接触反制类能力，在概率和目标关系都满足时，会把麻痹施加给来源。
     *
     * 这条测试覆盖 `all(...) + chance + target_relation + add_status(source)` 组合。
     */
    @Test
    fun shouldApplyStaticParalysisToSourceWhenLoadedFixtureChanceSucceeds() {
        val effect =
            JsonEffectDefinitionBattleDataLoader(
                resourcePaths = listOf("data/battle/fixtures/ability/static.json"),
            ).loadEffects().single()
        val rule = requireNotNull(effect.hooks[StandardHookNames.ON_HIT]).single()

        val context =
            io.github.lishangbu.avalon.game.battle.engine.event.EventContext(
                hookName = StandardHookNames.ON_HIT,
                battle = BattleState(id = "battle-static-1", formatId = "singles", turn = 3),
                self = UnitState(id = "p2a", currentHp = 100, maxHp = 100, abilityId = "static"),
                source = UnitState(id = "p1a", currentHp = 120, maxHp = 120),
                field = FieldState(),
                effect = effect,
                attributes =
                    mapOf(
                        "chanceRoll" to 12,
                        "targetRelation" to "foe",
                    ),
            )

        val result = processor.process(rule, context)

        assertTrue(result.matched)
        assertEquals(
            listOf(SetStatusMutation(StandardTargetSelectorIds.SOURCE, "par")),
            result.mutations,
        )
    }
}
