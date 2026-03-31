package io.github.lishangbu.avalon.game.battle.engine.runtime.processor

import io.github.lishangbu.avalon.game.battle.engine.event.StandardHookNames
import io.github.lishangbu.avalon.game.battle.engine.loader.JsonEffectDefinitionBattleDataLoader
import io.github.lishangbu.avalon.game.battle.engine.model.BattleState
import io.github.lishangbu.avalon.game.battle.engine.model.FieldState
import io.github.lishangbu.avalon.game.battle.engine.model.UnitState
import io.github.lishangbu.avalon.game.battle.engine.mutation.HealMutation
import io.github.lishangbu.avalon.game.battle.engine.registry.memory.StandardActionExecutorRegistryFactory
import io.github.lishangbu.avalon.game.battle.engine.registry.memory.StandardConditionInterpreterRegistryFactory
import io.github.lishangbu.avalon.game.battle.engine.runtime.DefaultHookRuleProcessor
import io.github.lishangbu.avalon.game.battle.engine.type.StandardTargetSelectorIds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * 关注点：
 * - `DefaultHookRuleProcessor` 在真实 `recover` 样例上是否能正确生成回复 mutation。
 */
class DefaultHookRuleProcessorRecoverFixtureTest {
    private val processor =
        DefaultHookRuleProcessor(
            conditionInterpreterRegistry = StandardConditionInterpreterRegistryFactory.create(),
            actionExecutorRegistry = StandardActionExecutorRegistryFactory.create(),
        )

    /**
     * 验证：
     * - `recover` 这种纯回复类招式，能够在 `on_hit` 产生自我回复 mutation。
     *
     * 这个测试确保 `heal` 动作在真实 fixture 上可用，而不是只在 schema 中存在。
     */
    @Test
    fun shouldApplyRecoverHealWhenLoadingRecoverFixture() {
        val effect =
            JsonEffectDefinitionBattleDataLoader(
                resourcePaths = listOf("data/battle/fixtures/move/recover.json"),
            ).loadEffects().single()
        val rule = requireNotNull(effect.hooks[StandardHookNames.ON_HIT]).single()

        val context =
            io.github.lishangbu.avalon.game.battle.engine.event.EventContext(
                hookName = StandardHookNames.ON_HIT,
                battle = BattleState(id = "battle-recover-1", formatId = "singles", turn = 5),
                self = UnitState(id = "p1a", currentHp = 60, maxHp = 120),
                field = FieldState(),
                effect = effect,
            )

        val result = processor.process(rule, context)

        assertEquals(
            listOf(HealMutation(StandardTargetSelectorIds.SELF, "max_hp_ratio", 0.5)),
            result.mutations,
        )
    }
}
