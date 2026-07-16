package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleStatusApplication
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.ElementEffectivenessChart
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

class BattleOpponentBerryPreventionAbilityTests {
	@Test
	fun `unnerve prevents an opponent resistance berry from being consumed`() {
		val skill = damagingSkill(skillId = 4802, elementId = 10)
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant(
						"unnerve-holder",
						100,
						skill = skill,
						abilityEffects = listOf(BattleAbilityEffect.OpponentBerryConsumptionPrevention()),
					),
					second = participant(
						"berry-holder",
						50,
						elementId = 12,
						itemId = 4802,
						itemEffects = listOf(
							BattleItemEffect.BerryMarker(),
							BattleItemEffect.ElementDamageReduction(10, 0.5),
						),
					),
					rules = neutralRules().copy(
						elementChart = ElementEffectivenessChart(mapOf(10L to mapOf(12L to 2.0))),
					),
				),
			),
			listOf(BattleAction.UseSkill("unnerve-holder", skill.skillId, "berry-holder")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		assertEquals(38, resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount)
		assertEquals(4802, resolved.participant("berry-holder")?.itemId)
	}

	@Test
	fun `unnerve prevents an opponent low hp berry from being consumed`() {
		val skill = damagingSkill(skillId = 4800, power = 40)
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant(
						"unnerve-holder",
						100,
						skill = skill,
						abilityEffects = listOf(BattleAbilityEffect.OpponentBerryConsumptionPrevention()),
					),
					second = participant(
						"berry-holder",
						50,
						currentHp = 50,
						itemId = 4800,
						itemEffects = listOf(BattleItemEffect.BerryMarker(), BattleItemEffect.LowHpHeal(healDenominator = 4)),
					),
				),
			),
			listOf(BattleAction.UseSkill("unnerve-holder", skill.skillId, "berry-holder")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		assertEquals(22, resolved.participant("berry-holder")?.currentHp)
		assertEquals(4800, resolved.participant("berry-holder")?.itemId)
	}

	@Test
	fun `unnerve prevents an opponent status cure berry from being consumed`() {
		val skill = damagingSkill(
			skillId = 4801,
			damageClass = BattleDamageClass.STATUS,
			power = null,
			statusApplications = listOf(
				BattleStatusApplication(BattleMajorStatus.POISON, BattleEffectTarget.TARGET, 100),
			),
		)
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant(
						"unnerve-holder",
						100,
						skill = skill,
						abilityEffects = listOf(BattleAbilityEffect.OpponentBerryConsumptionPrevention()),
					),
					second = participant(
						"berry-holder",
						50,
						itemId = 4801,
						itemEffects = listOf(
							BattleItemEffect.BerryMarker(),
							BattleItemEffect.MajorStatusCure(setOf(BattleMajorStatus.POISON), true),
						),
					),
				),
			),
			listOf(BattleAction.UseSkill("unnerve-holder", skill.skillId, "berry-holder")),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals(BattleMajorStatus.POISON, resolved.participant("berry-holder")?.majorStatus)
		assertEquals(4801, resolved.participant("berry-holder")?.itemId)
	}
}
