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
 * - `DefaultHookRuleProcessor` 在真实 `tangled-feet` 样例上是否能正确读取挥发状态并提升回避 relay。
 */
class DefaultHookRuleProcessorTangledFeetFixtureTest {
    private val processor =
        DefaultHookRuleProcessor(
            conditionInterpreterRegistry = StandardConditionInterpreterRegistryFactory.create(),
            actionExecutorRegistry = StandardActionExecutorRegistryFactory.create(),
        )

    /**
     * 验证：
     * - `tangled-feet` 在持有者处于混乱挥发状态时，会提升回避 relay。
     *
     * 这条测试覆盖 `has_volatile` 条件和 evasion 修正链。
     */
    @Test
    fun shouldApplyTangledFeetEvasionBoostWhenHolderIsConfused() {
        val effect =
            JsonEffectDefinitionBattleDataLoader(
                resourcePaths = listOf("data/battle/fixtures/ability/tangled-feet.json"),
            ).loadEffects().single()
        val rule = requireNotNull(effect.hooks[StandardHookNames.ON_MODIFY_EVASION]).single()

        val context =
            io.github.lishangbu.avalon.game.battle.engine.event.EventContext(
                hookName = StandardHookNames.ON_MODIFY_EVASION,
                battle = BattleState(id = "battle-tangled-1", formatId = "singles", turn = 4),
                self =
                    UnitState(
                        id = "p1a",
                        currentHp = 100,
                        maxHp = 100,
                        abilityId = "tangled-feet",
                        volatileIds = setOf("confusion"),
                    ),
                field = FieldState(),
                effect = effect,
                relay = 100.0,
            )

        val result = processor.process(rule, context)

        assertTrue(result.matched)
        assertEquals(200.0, result.relay)
    }
}
