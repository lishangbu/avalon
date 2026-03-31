package io.github.lishangbu.avalon.game.battle.engine.runtime.processor

import io.github.lishangbu.avalon.game.battle.engine.event.StandardHookNames
import io.github.lishangbu.avalon.game.battle.engine.loader.JsonEffectDefinitionBattleDataLoader
import io.github.lishangbu.avalon.game.battle.engine.model.BattleState
import io.github.lishangbu.avalon.game.battle.engine.model.FieldState
import io.github.lishangbu.avalon.game.battle.engine.model.UnitState
import io.github.lishangbu.avalon.game.battle.engine.registry.memory.StandardActionExecutorRegistryFactory
import io.github.lishangbu.avalon.game.battle.engine.registry.memory.StandardConditionInterpreterRegistryFactory
import io.github.lishangbu.avalon.game.battle.engine.runtime.DefaultHookRuleProcessor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 关注点：
 * - `DefaultHookRuleProcessor` 在真实 `electro-ball` 样例上是否能正确处理 base power 修正。
 */
class DefaultHookRuleProcessorElectroBallFixtureTest {
    private val processor =
        DefaultHookRuleProcessor(
            conditionInterpreterRegistry = StandardConditionInterpreterRegistryFactory.create(),
            actionExecutorRegistry = StandardActionExecutorRegistryFactory.create(),
        )

    /**
     * 验证：
     * - 从真实 fixture 加载出来的 `electro-ball` 规则，能够在 `on_modify_base_power` 阶段按速度阈值提升 relay。
     *
     * 这个测试对应的是：
     * - loader 能正确解析 `stat_compare`
     * - 条件解释器能读取 self 的 speed
     * - `modify_multiplier` 动作能真正改写威力 relay
     */
    @Test
    fun shouldApplyElectroBallBasePowerBoostWhenSpeedThresholdIsReached() {
        val effect =
            JsonEffectDefinitionBattleDataLoader(
                resourcePaths = listOf("data/battle/fixtures/move/electro-ball.json"),
            ).loadEffects().single()
        val rule = requireNotNull(effect.hooks[StandardHookNames.ON_MODIFY_BASE_POWER]).single()

        val context =
            io.github.lishangbu.avalon.game.battle.engine.event.EventContext(
                hookName = StandardHookNames.ON_MODIFY_BASE_POWER,
                battle = BattleState(id = "battle-electro-1", formatId = "singles", turn = 3),
                self =
                    UnitState(
                        id = "p1a",
                        currentHp = 100,
                        maxHp = 100,
                        stats = mapOf("spe" to 180),
                    ),
                target = UnitState(id = "p2a", currentHp = 100, maxHp = 100),
                field = FieldState(),
                effect = effect,
                relay = 100.0,
            )

        val result = processor.process(rule, context)

        assertTrue(result.matched)
        assertEquals(150.0, result.relay)
    }
}
