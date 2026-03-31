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
 * - `DefaultHookRuleProcessor` 在真实 `ember` 样例上是否能正确处理概率性灼伤附加。
 */
class DefaultHookRuleProcessorEmberFixtureTest {
    private val processor =
        DefaultHookRuleProcessor(
            conditionInterpreterRegistry = StandardConditionInterpreterRegistryFactory.create(),
            actionExecutorRegistry = StandardActionExecutorRegistryFactory.create(),
        )

    /**
     * 验证：
     * - `ember` 的二次效果规则在概率满足时，会通过 `on_hit` 产出灼伤 mutation。
     *
     * 这个测试覆盖：
     * - fixture 加载
     * - `chance` 条件解释
     * - `add_status` 动作执行
     */
    @Test
    fun shouldApplyEmberBurnWhenLoadedFixtureChanceSucceeds() {
        val effect =
            JsonEffectDefinitionBattleDataLoader(
                resourcePaths = listOf("data/battle/fixtures/move/ember.json"),
            ).loadEffects().single()
        val rule = requireNotNull(effect.hooks[StandardHookNames.ON_HIT]).single()

        val context =
            io.github.lishangbu.avalon.game.battle.engine.event.EventContext(
                hookName = StandardHookNames.ON_HIT,
                battle = BattleState(id = "battle-ember-1", formatId = "singles", turn = 2),
                self = UnitState(id = "p1a", currentHp = 110, maxHp = 110),
                target = UnitState(id = "p2a", currentHp = 90, maxHp = 90),
                field = FieldState(),
                effect = effect,
                attributes = mapOf("chanceRoll" to 3),
            )

        val result = processor.process(rule, context)

        assertTrue(result.matched)
        assertEquals(
            listOf(SetStatusMutation(StandardTargetSelectorIds.TARGET, "brn")),
            result.mutations,
        )
    }
}
