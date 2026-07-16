package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleFixedDamage
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.ElementEffectivenessChart
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

class BattleBerryEffectMultiplierAbilityTests {
	@Test
	fun `ripen doubles the random stat stages produced by a consumed berry`() {
		val skill = damagingSkill(skillId = 1099, power = null, fixedDamage = BattleFixedDamage.FixedAmount(1))
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("attacker", 100, skill = skill),
					second = participant(
						"ripen-holder",
						50,
						currentHp = 25,
						abilityEffects = listOf(BattleAbilityEffect.BerryEffectMultiplier(2.0)),
						itemId = 1099,
						itemEffects = listOf(
							BattleItemEffect.BerryMarker(),
							BattleItemEffect.LowHpRandomStatStageBoost(setOf(BattleStat.ATTACK), 2),
						),
					),
				),
			),
			listOf(BattleAction.UseSkill("attacker", skill.skillId, "ripen-holder")),
			ScriptedBattleRandom(listOf(0)),
		)

		assertEquals(4, resolved.participant("ripen-holder")?.statStage(BattleStat.ATTACK))
	}

	@Test
	fun `ripen doubles the critical hit stage produced by a consumed berry`() {
		val skill = damagingSkill(skillId = 1098, power = 40)
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("attacker", 100, skill = skill),
					second = participant(
						"ripen-holder",
						50,
						currentHp = 50,
						abilityEffects = listOf(BattleAbilityEffect.BerryEffectMultiplier(2.0)),
						itemId = 1098,
						itemEffects = listOf(
							BattleItemEffect.BerryMarker(),
							BattleItemEffect.LowHpCriticalHitStageBoost(1),
						),
					),
				),
			),
			listOf(BattleAction.UseSkill("attacker", skill.skillId, "ripen-holder")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		assertEquals(2, resolved.participant("ripen-holder")?.criticalHitStageBonus)
	}

	@Test
	fun `ripen doubles only the accuracy increase produced by a consumed berry`() {
		val skill = damagingSkill(skillId = 1097, power = null, fixedDamage = BattleFixedDamage.FixedAmount(1))
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("attacker", 100, skill = skill),
					second = participant(
						"ripen-holder",
						50,
						currentHp = 25,
						abilityEffects = listOf(BattleAbilityEffect.BerryEffectMultiplier(2.0)),
						itemId = 1097,
						itemEffects = listOf(
							BattleItemEffect.BerryMarker(),
							BattleItemEffect.LowHpNextSkillAccuracyBoost(1.2),
						),
					),
				),
			),
			listOf(BattleAction.UseSkill("attacker", skill.skillId, "ripen-holder")),
			ScriptedBattleRandom(listOf(1)),
		)

		assertEquals(1.4, requireNotNull(resolved.participant("ripen-holder")).nextSkillAccuracyMultiplier, 0.0000001)
	}

	@Test
	fun `ripen doubles the reduction produced by a resistance berry`() {
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("attacker", 100, elementId = 2, skill = damagingSkill(elementId = 10)),
					second = participant(
						"ripen-holder",
						50,
						elementId = 12,
						abilityEffects = listOf(BattleAbilityEffect.BerryEffectMultiplier(2.0)),
						itemId = 1096,
						itemEffects = listOf(
							BattleItemEffect.BerryMarker(),
							BattleItemEffect.ElementDamageReduction(elementId = 10, multiplier = 0.5),
						),
					),
					rules = neutralRules().copy(
						elementChart = ElementEffectivenessChart(mapOf(10L to mapOf(12L to 2.0))),
					),
				),
			),
			listOf(BattleAction.UseSkill("attacker", 1, "ripen-holder")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		assertEquals(9, resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount)
		assertEquals(0.25, resolved.events.filterIsInstance<BattleEvent.DamageReducedByItem>().single().multiplier)
	}

	@Test
	fun `ripen doubles the stat stages produced by a consumed berry`() {
		val skill = damagingSkill(skillId = 1096, power = 40)
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("attacker", 100, skill = skill),
					second = participant(
						"ripen-holder",
						50,
						currentHp = 50,
						abilityEffects = listOf(BattleAbilityEffect.BerryEffectMultiplier(2.0)),
						itemId = 1096,
						itemEffects = listOf(
							BattleItemEffect.BerryMarker(),
							BattleItemEffect.LowHpStatStageBoost(BattleStat.ATTACK, 1),
						),
					),
				),
			),
			listOf(BattleAction.UseSkill("attacker", skill.skillId, "ripen-holder")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		assertEquals(2, resolved.participant("ripen-holder")?.statStage(BattleStat.ATTACK))
	}

	@Test
	fun `ripen doubles the healing produced by a consumed berry`() {
		val skill = damagingSkill(skillId = 1095, power = 40)
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("attacker", 100, skill = skill),
					second = participant(
						"ripen-holder",
						50,
						currentHp = 60,
						abilityEffects = listOf(BattleAbilityEffect.BerryEffectMultiplier(2.0)),
						itemId = 1095,
						itemEffects = listOf(
							BattleItemEffect.BerryMarker(),
							BattleItemEffect.LowHpHeal(healDenominator = 4),
						),
					),
				),
			),
			listOf(BattleAction.UseSkill("attacker", skill.skillId, "ripen-holder")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		assertEquals(82, resolved.participant("ripen-holder")?.currentHp)
	}
}
