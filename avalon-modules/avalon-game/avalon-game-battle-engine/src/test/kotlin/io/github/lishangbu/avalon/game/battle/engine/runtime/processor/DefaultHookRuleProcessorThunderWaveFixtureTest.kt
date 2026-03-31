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
 * - `DefaultHookRuleProcessor` 在真实 `thunder-wave` 样例上是否能正确附加麻痹。
 */
class DefaultHookRuleProcessorThunderWaveFixtureTest {
    private val processor =
        DefaultHookRuleProcessor(
            conditionInterpreterRegistry = StandardConditionInterpreterRegistryFactory.create(),
            actionExecutorRegistry = StandardActionExecutorRegistryFactory.create(),
        )

    /**
     * 验证：
     * - `thunder-wave` 在目标不是地面属性时，会成功产出麻痹 mutation。
     *
     * 这条测试对应的是“条件命中后稳定状态附加”的基准路径。
     */
    @Test
    fun shouldApplyThunderWaveWhenLoadedFixtureTargetIsNotGroundType() {
        val effect =
            JsonEffectDefinitionBattleDataLoader(
                resourcePaths = listOf("data/battle/fixtures/move/thunder-wave.json"),
            ).loadEffects().single()
        val rule = requireNotNull(effect.hooks[StandardHookNames.ON_HIT]).single()

        val context =
            io.github.lishangbu.avalon.game.battle.engine.event.EventContext(
                hookName = StandardHookNames.ON_HIT,
                battle = BattleState(id = "battle-twave-1", formatId = "singles", turn = 2),
                self = UnitState(id = "p1a", currentHp = 120, maxHp = 120),
                target = UnitState(id = "p2a", currentHp = 100, maxHp = 100, typeIds = setOf("water")),
                field = FieldState(),
                effect = effect,
            )

        val result = processor.process(rule, context)

        assertTrue(result.matched)
        assertEquals(
            listOf(SetStatusMutation(StandardTargetSelectorIds.TARGET, "par")),
            result.mutations,
        )
    }
}
