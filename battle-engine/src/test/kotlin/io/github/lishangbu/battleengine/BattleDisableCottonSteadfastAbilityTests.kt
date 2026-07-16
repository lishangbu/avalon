package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

class BattleDisableCottonSteadfastAbilityTests {
	@Test
	fun `cursed body disables the attacking skill after a successful roll`() {
		val skill = damagingSkill(skillId = 951)
		val resolved = BattleEngine().let { engine ->
			engine.resolveTurn(
				engine.start(
					initialState(
						first = participant("attacker", 100, skill = skill),
						second = participant(
							"holder",
							50,
							abilityEffects = listOf(BattleAbilityEffect.ReceivedDamageDisableAttackerSkill(30, 4)),
						),
					),
				),
				listOf(BattleAction.UseSkill("attacker", skill.skillId, "holder")),
				ScriptedBattleRandom(listOf(1, 15, 0)),
			)
		}

		assertEquals(skill.skillId, resolved.participant("attacker")?.disabledSkillId)
		assertEquals(3, resolved.participant("attacker")?.disabledSkillTurnsRemaining)
	}

	@Test
	fun `cotton down lowers every other active participant speed`() {
		val skill = damagingSkill(skillId = 952)
		val resolved = BattleEngine().let { engine ->
			engine.resolveTurn(
				engine.start(
					initialState(
						first = participant("attacker", 100, skill = skill),
						second = participant(
							"holder",
							50,
							abilityEffects = listOf(
								BattleAbilityEffect.ReceivedDamageAllOtherStatStageChange(BattleStat.SPEED, -1),
							),
						),
					),
				),
				listOf(BattleAction.UseSkill("attacker", skill.skillId, "holder")),
				ScriptedBattleRandom(listOf(1, 15)),
			)
		}

		assertEquals(-1, resolved.participant("attacker")?.statStage(BattleStat.SPEED))
	}

	@Test
	fun `steadfast raises speed when flinch prevents the action`() {
		val skill = damagingSkill(skillId = 953)
		val holder = participant(
			"holder",
			100,
			skill = skill,
			abilityEffects = listOf(BattleAbilityEffect.FlinchStatStageBoost(BattleStat.SPEED, 1)),
		).copy(flinched = true)
		val resolved = BattleEngine().let { engine ->
			engine.resolveTurn(
				engine.start(initialState(first = holder, second = participant("target", 50))),
				listOf(BattleAction.UseSkill("holder", skill.skillId, "target")),
				ScriptedBattleRandom(emptyList()),
			)
		}

		assertEquals(1, resolved.participant("holder")?.statStage(BattleStat.SPEED))
	}
}
