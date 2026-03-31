package io.github.lishangbu.avalon.game.service.effect

import io.github.lishangbu.avalon.game.battle.engine.dsl.action.AddStatusActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.BoostActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.ConsumeItemActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.HealActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.condition.ChanceConditionNode
import io.github.lishangbu.avalon.game.battle.engine.event.StandardHookNames
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SmartBattleEffectAssemblerTest {
    @Test
    fun shouldAssembleDatasetMoveWithSecondaryStatus() {
        val effect =
            SmartBattleEffectAssembler.fromMove(
                MoveImportRecord(
                    internalName = "thunderbolt",
                    name = "十万伏特",
                    typeInternalName = "electric",
                    damageClassInternalName = "special",
                    targetInternalName = "selected-pokemon",
                    accuracy = 100,
                    effectChance = 10,
                    pp = 15,
                    priority = 0,
                    power = 90,
                    shortEffect = "May paralyze the target.",
                    effect = "Inflicts damage and may paralyze the target.",
                    ailmentInternalName = "paralysis",
                    ailmentChance = 10,
                    healing = null,
                    drain = null,
                ),
            )

        assertEquals("thunderbolt", effect.id)
        assertTrue("damaging" in effect.tags)
        assertTrue("special" in effect.tags)
        assertEquals("selected-pokemon", effect.data["target"])
        val hitRule = effect.hooks[StandardHookNames.ON_HIT]?.single()
        assertNotNull(hitRule)
        assertIs<ChanceConditionNode>(hitRule.condition)
        assertIs<AddStatusActionNode>(hitRule.thenActions.single())
        assertEquals("par", (hitRule.thenActions.single() as AddStatusActionNode).value)
    }

    @Test
    fun shouldAssembleStaticAbilityWithReactiveHook() {
        val effect =
            SmartBattleEffectAssembler.fromAbility(
                AbilityImportRecord(
                    internalName = "static",
                    name = "静电",
                    effect = "Has a 30% chance of paralyzing attacking Pokemon on contact.",
                    introduction = null,
                ),
            )

        assertEquals("static", effect.id)
        val hitRule = effect.hooks[StandardHookNames.ON_HIT]?.single()
        assertNotNull(hitRule)
        assertEquals(1, hitRule.thenActions.size)
        assertIs<AddStatusActionNode>(hitRule.thenActions.single())
    }

    @Test
    fun shouldAssembleSpeedBoostAbility() {
        val effect =
            SmartBattleEffectAssembler.fromAbility(
                AbilityImportRecord(
                    internalName = "speed-boost",
                    name = "加速",
                    effect = "Its Speed stat is boosted every turn.",
                    introduction = null,
                ),
            )

        val residualRule = effect.hooks[StandardHookNames.ON_RESIDUAL]?.single()
        assertNotNull(residualRule)
        val action = residualRule.thenActions.single()
        assertIs<BoostActionNode>(action)
        assertEquals(1, action.stats["speed"])
    }

    @Test
    fun shouldAssembleHealingBerryItem() {
        val effect =
            SmartBattleEffectAssembler.fromItem(
                ItemImportRecord(
                    internalName = "sitrus-berry",
                    name = "文柚果",
                    shortEffect = "Consumed when HP is low, restores 1/4 max HP.",
                    effect = "When at 1/2 HP or less, restores 1/4 of max HP.",
                    text = null,
                    attributeInternalNames = setOf("holdable", "holdable-passive"),
                    flingEffectInternalName = null,
                ),
            )

        val residualRule = effect.hooks[StandardHookNames.ON_RESIDUAL]?.single()
        assertNotNull(residualRule)
        assertEquals(2, residualRule.thenActions.size)
        assertIs<ConsumeItemActionNode>(residualRule.thenActions[0])
        val healAction = residualRule.thenActions[1]
        assertIs<HealActionNode>(healAction)
        assertEquals("max_hp_ratio", healAction.mode)
        assertEquals(0.25, healAction.value)
    }
}
