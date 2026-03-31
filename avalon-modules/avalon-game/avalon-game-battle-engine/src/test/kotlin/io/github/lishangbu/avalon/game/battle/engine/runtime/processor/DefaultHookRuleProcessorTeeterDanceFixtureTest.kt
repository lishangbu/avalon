package io.github.lishangbu.avalon.game.battle.engine.runtime.processor

import io.github.lishangbu.avalon.game.battle.engine.event.StandardHookNames
import io.github.lishangbu.avalon.game.battle.engine.loader.JsonEffectDefinitionBattleDataLoader
import io.github.lishangbu.avalon.game.battle.engine.model.BattleState
import io.github.lishangbu.avalon.game.battle.engine.model.FieldState
import io.github.lishangbu.avalon.game.battle.engine.model.UnitState
import io.github.lishangbu.avalon.game.battle.engine.mutation.AddVolatileMutation
import io.github.lishangbu.avalon.game.battle.engine.registry.memory.StandardActionExecutorRegistryFactory
import io.github.lishangbu.avalon.game.battle.engine.registry.memory.StandardConditionInterpreterRegistryFactory
import io.github.lishangbu.avalon.game.battle.engine.runtime.DefaultHookRuleProcessor
import io.github.lishangbu.avalon.game.battle.engine.type.StandardTargetSelectorIds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * 关注点：
 * - `DefaultHookRuleProcessor` 在真实 `teeter-dance` 样例上是否能正确生成群体混乱挥发状态 mutation。
 */
class DefaultHookRuleProcessorTeeterDanceFixtureTest {
    private val processor =
        DefaultHookRuleProcessor(
            conditionInterpreterRegistry = StandardConditionInterpreterRegistryFactory.create(),
            actionExecutorRegistry = StandardActionExecutorRegistryFactory.create(),
        )

    /**
     * 验证：
     * - `teeter-dance` 能对群体 selector 产出附加混乱挥发状态的 mutation。
     *
     * 这条测试覆盖群体目标与 `add_volatile` 的组合。
     */
    @Test
    fun shouldApplyTeeterDanceConfusionWhenLoadingTeeterDanceFixture() {
        val effect =
            JsonEffectDefinitionBattleDataLoader(
                resourcePaths = listOf("data/battle/fixtures/move/teeter-dance.json"),
            ).loadEffects().single()
        val rule = requireNotNull(effect.hooks[StandardHookNames.ON_HIT]).single()

        val context =
            io.github.lishangbu.avalon.game.battle.engine.event.EventContext(
                hookName = StandardHookNames.ON_HIT,
                battle = BattleState(id = "battle-teeter-1", formatId = "doubles", turn = 3),
                self = UnitState(id = "p1a", currentHp = 120, maxHp = 120),
                field = FieldState(),
                effect = effect,
            )

        val result = processor.process(rule, context)

        assertEquals(
            listOf(AddVolatileMutation(StandardTargetSelectorIds.ALL, "confusion")),
            result.mutations,
        )
    }
}
