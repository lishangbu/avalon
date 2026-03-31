package io.github.lishangbu.avalon.game.battle.engine.runtime.processor

import io.github.lishangbu.avalon.game.battle.engine.event.StandardHookNames
import io.github.lishangbu.avalon.game.battle.engine.loader.JsonEffectDefinitionBattleDataLoader
import io.github.lishangbu.avalon.game.battle.engine.model.BattleState
import io.github.lishangbu.avalon.game.battle.engine.model.FieldState
import io.github.lishangbu.avalon.game.battle.engine.model.UnitState
import io.github.lishangbu.avalon.game.battle.engine.mutation.BoostMutation
import io.github.lishangbu.avalon.game.battle.engine.registry.memory.StandardActionExecutorRegistryFactory
import io.github.lishangbu.avalon.game.battle.engine.registry.memory.StandardConditionInterpreterRegistryFactory
import io.github.lishangbu.avalon.game.battle.engine.runtime.DefaultHookRuleProcessor
import io.github.lishangbu.avalon.game.battle.engine.type.StandardTargetSelectorIds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 关注点：
 * - `DefaultHookRuleProcessor` 在真实 `feather-dance` 样例上是否能正确处理对敌方的负向 boost。
 */
class DefaultHookRuleProcessorFeatherDanceFixtureTest {
    private val processor =
        DefaultHookRuleProcessor(
            conditionInterpreterRegistry = StandardConditionInterpreterRegistryFactory.create(),
            actionExecutorRegistry = StandardActionExecutorRegistryFactory.create(),
        )

    /**
     * 验证：
     * - `feather-dance` 这类对敌方单体降攻的规则，能在目标关系满足 `foe` 时生成负向 boost mutation。
     *
     * 这个测试用来证明：
     * - `target_relation` 条件可用
     * - `boost` 动作既支持正向也支持负向 stage 变化
     */
    @Test
    fun shouldApplyFeatherDanceAttackDropWhenLoadingFeatherDanceFixture() {
        val effect =
            JsonEffectDefinitionBattleDataLoader(
                resourcePaths = listOf("data/battle/fixtures/move/feather-dance.json"),
            ).loadEffects().single()
        val rule = requireNotNull(effect.hooks[StandardHookNames.ON_HIT]).single()

        val context =
            io.github.lishangbu.avalon.game.battle.engine.event.EventContext(
                hookName = StandardHookNames.ON_HIT,
                battle = BattleState(id = "battle-feather-1", formatId = "singles", turn = 3),
                self = UnitState(id = "p1a", currentHp = 120, maxHp = 120),
                target = UnitState(id = "p2a", currentHp = 100, maxHp = 100),
                field = FieldState(),
                effect = effect,
                attributes = mapOf("targetRelation" to "foe"),
            )

        val result = processor.process(rule, context)

        assertTrue(result.matched)
        assertEquals(
            listOf(BoostMutation(StandardTargetSelectorIds.TARGET, mapOf("atk" to -2))),
            result.mutations,
        )
    }
}
