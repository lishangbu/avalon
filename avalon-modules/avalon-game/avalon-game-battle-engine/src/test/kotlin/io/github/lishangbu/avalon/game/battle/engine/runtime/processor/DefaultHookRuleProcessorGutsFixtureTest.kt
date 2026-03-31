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
 * - `DefaultHookRuleProcessor` 在真实 `guts` 样例上是否能正确提升攻击 relay。
 */
class DefaultHookRuleProcessorGutsFixtureTest {
    private val processor =
        DefaultHookRuleProcessor(
            conditionInterpreterRegistry = StandardConditionInterpreterRegistryFactory.create(),
            actionExecutorRegistry = StandardActionExecutorRegistryFactory.create(),
        )

    /**
     * 验证：
     * - `guts` 在持有者处于异常状态时，会在 `on_modify_attack` 阶段提高攻击 relay。
     *
     * 这个测试覆盖：
     * - `has_status` 条件
     * - `modify_multiplier` 对数值 relay 的修改
     */
    @Test
    fun shouldApplyGutsAttackBoostWhenLoadedFixtureHolderHasStatus() {
        val effect =
            JsonEffectDefinitionBattleDataLoader(
                resourcePaths = listOf("data/battle/fixtures/ability/guts.json"),
            ).loadEffects().single()
        val rule = requireNotNull(effect.hooks[StandardHookNames.ON_MODIFY_ATTACK]).single()

        val context =
            io.github.lishangbu.avalon.game.battle.engine.event.EventContext(
                hookName = StandardHookNames.ON_MODIFY_ATTACK,
                battle = BattleState(id = "battle-guts-1", formatId = "singles", turn = 6),
                self =
                    UnitState(
                        id = "p1a",
                        currentHp = 90,
                        maxHp = 150,
                        statusId = "brn",
                        abilityId = "guts",
                    ),
                field = FieldState(),
                effect = effect,
                relay = 100.0,
            )

        val result = processor.process(rule, context)

        assertTrue(result.matched)
        assertEquals(150.0, result.relay)
    }
}
