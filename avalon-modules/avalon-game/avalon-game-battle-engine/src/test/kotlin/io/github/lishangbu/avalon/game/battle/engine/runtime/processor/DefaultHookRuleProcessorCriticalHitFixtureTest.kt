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
 * - `on_modify_crit_ratio` fixture 能真实改写 critical ratio relay。
 */
class DefaultHookRuleProcessorCriticalHitFixtureTest {
    private val processor =
        DefaultHookRuleProcessor(
            conditionInterpreterRegistry = StandardConditionInterpreterRegistryFactory.create(),
            actionExecutorRegistry = StandardActionExecutorRegistryFactory.create(),
        )

    @Test
    fun shouldIncreaseCriticalRatioWhenAttackerHasSuperLuck() {
        val effect =
            JsonEffectDefinitionBattleDataLoader(
                resourcePaths = listOf("data/battle/fixtures/ability/super-luck.json"),
            ).loadEffects().single()
        val rule = requireNotNull(effect.hooks[StandardHookNames.ON_MODIFY_CRIT_RATIO]).single()

        val context =
            io.github.lishangbu.avalon.game.battle.engine.event.EventContext(
                hookName = StandardHookNames.ON_MODIFY_CRIT_RATIO,
                battle = BattleState(id = "battle-super-luck-1", formatId = "singles", turn = 6),
                self =
                    UnitState(
                        id = "p1a",
                        currentHp = 100,
                        maxHp = 100,
                        abilityId = "super-luck",
                    ),
                target = UnitState(id = "p2a", currentHp = 100, maxHp = 100),
                field = FieldState(),
                effect = effect,
                relay = 1,
            )

        val result = processor.process(rule, context)

        assertTrue(result.matched)
        assertEquals(2.0, result.relay as Double, 1e-9)
    }
}
