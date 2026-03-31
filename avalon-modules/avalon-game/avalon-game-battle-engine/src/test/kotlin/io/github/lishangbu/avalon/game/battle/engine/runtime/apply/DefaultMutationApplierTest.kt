package io.github.lishangbu.avalon.game.battle.engine.runtime.apply

import io.github.lishangbu.avalon.game.battle.engine.event.StandardHookNames
import io.github.lishangbu.avalon.game.battle.engine.loader.JsonEffectDefinitionBattleDataLoader
import io.github.lishangbu.avalon.game.battle.engine.model.BattleState
import io.github.lishangbu.avalon.game.battle.engine.model.FieldState
import io.github.lishangbu.avalon.game.battle.engine.model.UnitState
import io.github.lishangbu.avalon.game.battle.engine.registry.memory.StandardActionExecutorRegistryFactory
import io.github.lishangbu.avalon.game.battle.engine.registry.memory.StandardConditionInterpreterRegistryFactory
import io.github.lishangbu.avalon.game.battle.engine.runtime.DefaultHookRuleProcessor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 关注点：
 * - `DefaultMutationApplier` 是否能把结构化 mutation 正确提交回不可变状态快照。
 *
 * 这类测试关注状态提交层，不关心 mutation 是如何被规则处理器生成出来的。
 */
class DefaultMutationApplierTest {
    private val processor =
        DefaultHookRuleProcessor(
            conditionInterpreterRegistry = StandardConditionInterpreterRegistryFactory.create(),
            actionExecutorRegistry = StandardActionExecutorRegistryFactory.create(),
        )

    private val applier = DefaultMutationApplier()

    /**
     * 验证：
     * - `recover` 产生的 heal mutation 在 apply 层会真正把 self 的 HP 回写到最大值。
     *
     * 这说明 mutation 不只是被生成出来，而且已经能被状态提交层正确消费。
     */
    @Test
    fun shouldApplyRecoverMutationsWhenHealingSelf() {
        val effect =
            JsonEffectDefinitionBattleDataLoader(
                resourcePaths = listOf("data/battle/fixtures/move/recover.json"),
            ).loadEffects().single()
        val rule = requireNotNull(effect.hooks[StandardHookNames.ON_HIT]).single()
        val self = UnitState(id = "p1a", currentHp = 60, maxHp = 120)
        val context =
            io.github.lishangbu.avalon.game.battle.engine.event.EventContext(
                hookName = StandardHookNames.ON_HIT,
                battle = BattleState(id = "battle-apply-1", formatId = "singles", turn = 4),
                self = self,
                field = FieldState(),
                effect = effect,
            )

        val processResult = processor.process(rule, context)
        val applicationResult =
            applier.apply(
                mutations = processResult.mutations,
                context =
                    MutationApplicationContext(
                        battle = context.battle,
                        field = context.field ?: FieldState(),
                        units = mapOf(self.id to self),
                        selfId = self.id,
                    ),
            )

        assertEquals(120, applicationResult.units.getValue("p1a").currentHp)
    }

    /**
     * 验证：
     * - `soak` 的 change_type mutation 在 apply 层会真正改写目标属性集合。
     *
     * 这条测试覆盖状态提交层对属性集合的不可变回写。
     */
    @Test
    fun shouldApplySoakMutationsWhenChangingTargetTypes() {
        val effect =
            JsonEffectDefinitionBattleDataLoader(
                resourcePaths = listOf("data/battle/fixtures/move/soak.json"),
            ).loadEffects().single()
        val rule = requireNotNull(effect.hooks[StandardHookNames.ON_HIT]).single()
        val self = UnitState(id = "p1a", currentHp = 120, maxHp = 120)
        val target = UnitState(id = "p2a", currentHp = 100, maxHp = 100, typeIds = setOf("fire"))
        val context =
            io.github.lishangbu.avalon.game.battle.engine.event.EventContext(
                hookName = StandardHookNames.ON_HIT,
                battle = BattleState(id = "battle-apply-2", formatId = "singles", turn = 3),
                self = self,
                target = target,
                field = FieldState(),
                effect = effect,
            )

        val processResult = processor.process(rule, context)
        val applicationResult =
            applier.apply(
                mutations = processResult.mutations,
                context =
                    MutationApplicationContext(
                        battle = context.battle,
                        field = context.field ?: FieldState(),
                        units = mapOf(self.id to self, target.id to target),
                        selfId = self.id,
                        targetId = target.id,
                    ),
            )

        assertEquals(setOf("water"), applicationResult.units.getValue("p2a").typeIds)
    }

    /**
     * 验证：
     * - `haze` 的 clear_boosts mutation 在 apply 层会对 `all` selector 命中的单位全部清空 boosts。
     *
     * 这条测试覆盖群体 selector 在 mutation apply 层的解析。
     */
    @Test
    fun shouldApplyHazeMutationsWhenClearingAllBoosts() {
        val effect =
            JsonEffectDefinitionBattleDataLoader(
                resourcePaths = listOf("data/battle/fixtures/move/haze.json"),
            ).loadEffects().single()
        val rule = requireNotNull(effect.hooks[StandardHookNames.ON_HIT]).single()
        val self = UnitState(id = "p1a", currentHp = 120, maxHp = 120, boosts = mapOf("atk" to 2))
        val target = UnitState(id = "p2a", currentHp = 100, maxHp = 100, boosts = mapOf("def" to 3))
        val source = UnitState(id = "p3a", currentHp = 80, maxHp = 80, boosts = mapOf("spe" to 1))
        val context =
            io.github.lishangbu.avalon.game.battle.engine.event.EventContext(
                hookName = StandardHookNames.ON_HIT,
                battle = BattleState(id = "battle-apply-3", formatId = "triples", turn = 5),
                self = self,
                target = target,
                source = source,
                field = FieldState(),
                effect = effect,
            )

        val processResult = processor.process(rule, context)
        val applicationResult =
            applier.apply(
                mutations = processResult.mutations,
                context =
                    MutationApplicationContext(
                        battle = context.battle,
                        field = context.field ?: FieldState(),
                        units = mapOf(self.id to self, target.id to target, source.id to source),
                        selfId = self.id,
                        targetId = target.id,
                        sourceId = source.id,
                    ),
            )

        assertTrue(applicationResult.units.values.all { unit -> unit.boosts.isEmpty() })
    }

    /**
     * 验证：
     * - `sitrus-berry` 产生的多条 mutation 会按顺序一起应用：先消耗道具，再回复 HP。
     *
     * 这个测试证明 apply 层支持一个规则链产生多个结构化变更。
     */
    @Test
    fun shouldApplySitrusBerryMutationsWhenBerryTriggers() {
        val effect =
            JsonEffectDefinitionBattleDataLoader(
                resourcePaths = listOf("data/battle/fixtures/item/sitrus-berry.json"),
            ).loadEffects().single()
        val rule = requireNotNull(effect.hooks[StandardHookNames.ON_RESIDUAL]).single()
        val self = UnitState(id = "p1a", currentHp = 50, maxHp = 100, itemId = "sitrus-berry")
        val context =
            io.github.lishangbu.avalon.game.battle.engine.event.EventContext(
                hookName = StandardHookNames.ON_RESIDUAL,
                battle = BattleState(id = "battle-apply-4", formatId = "singles", turn = 6),
                self = self,
                field = FieldState(),
                effect = effect,
            )

        val processResult = processor.process(rule, context)
        val applicationResult =
            applier.apply(
                mutations = processResult.mutations,
                context =
                    MutationApplicationContext(
                        battle = context.battle,
                        field = context.field ?: FieldState(),
                        units = mapOf(self.id to self),
                        selfId = self.id,
                    ),
            )

        val updatedSelf = applicationResult.units.getValue("p1a")
        assertEquals(75, updatedSelf.currentHp)
        assertNull(updatedSelf.itemId)
    }

    /**
     * 验证：
     * - `TriggerEventMutation` 在 apply 层不会直接改状态，而是会被收集为后续 hook 触发请求。
     *
     * 这条测试保证 trigger-event 这种“流程型 mutation”不会被误当作状态变更处理。
     */
    @Test
    fun shouldCollectTriggeredHooksWhenApplyingTriggerEventMutation() {
        val result =
            applier.apply(
                mutations =
                    listOf(
                        io.github.lishangbu.avalon.game.battle.engine.mutation
                            .TriggerEventMutation(StandardHookNames.ON_AFTER_MOVE),
                    ),
                context =
                    MutationApplicationContext(
                        battle = BattleState(id = "battle-apply-5", formatId = "singles", turn = 1),
                        field = FieldState(),
                        units = emptyMap(),
                    ),
            )

        assertEquals(listOf(StandardHookNames.ON_AFTER_MOVE), result.triggeredHooks)
    }
}
