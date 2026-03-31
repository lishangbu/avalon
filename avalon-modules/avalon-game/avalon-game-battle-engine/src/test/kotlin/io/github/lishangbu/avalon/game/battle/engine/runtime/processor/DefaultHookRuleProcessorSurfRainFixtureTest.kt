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
 * - `DefaultHookRuleProcessor` 在真实 `surf-rain` 样例上是否能正确处理天气伤害修正。
 */
class DefaultHookRuleProcessorSurfRainFixtureTest {
    private val processor =
        DefaultHookRuleProcessor(
            conditionInterpreterRegistry = StandardConditionInterpreterRegistryFactory.create(),
            actionExecutorRegistry = StandardActionExecutorRegistryFactory.create(),
        )

    /**
     * 验证：
     * - `surf` 在雨天条件下，会在 `on_modify_damage` 阶段提升伤害 relay。
     *
     * 这条测试覆盖天气条件与伤害修正链的联动。
     */
    @Test
    fun shouldApplySurfRainDamageBoostWhenLoadingSurfRainFixture() {
        val effect =
            JsonEffectDefinitionBattleDataLoader(
                resourcePaths = listOf("data/battle/fixtures/move/surf-rain.json"),
            ).loadEffects().single()
        val rule = requireNotNull(effect.hooks[StandardHookNames.ON_MODIFY_DAMAGE]).single()

        val context =
            io.github.lishangbu.avalon.game.battle.engine.event.EventContext(
                hookName = StandardHookNames.ON_MODIFY_DAMAGE,
                battle = BattleState(id = "battle-surf-1", formatId = "singles", turn = 4),
                self = UnitState(id = "p1a", currentHp = 120, maxHp = 120),
                target = UnitState(id = "p2a", currentHp = 100, maxHp = 100),
                field = FieldState(weatherId = "rain"),
                effect = effect,
                relay = 100.0,
            )

        val result = processor.process(rule, context)

        assertTrue(result.matched)
        assertEquals(150.0, result.relay)
    }
}
