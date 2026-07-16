package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleStatusApplication
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

class BattlePpSleepSideAbilityTests {
	@Test
	fun `pressure makes a targeted opponent spend two pp`() {
		val skill = damagingSkill(skillId = 881)
		val resolved = BattleEngine().let { engine ->
			engine.resolveTurn(
				engine.start(
					initialState(
						first = participant("attacker", 100, skill = skill),
						second = participant(
							"holder",
							50,
							abilityEffects = listOf(BattleAbilityEffect.OpponentSkillPpCostIncrease(1)),
						),
					),
				),
				listOf(BattleAction.UseSkill("attacker", skill.skillId, "holder")),
				ScriptedBattleRandom(listOf(1, 15)),
			)
		}

		assertEquals(33, resolved.participant("attacker")?.skillSlot(skill.skillId)?.remainingPp)
	}

	@Test
	fun `early bird halves rolled sleep duration`() {
		val sleepSkill = damagingSkill(
			skillId = 882,
			damageClass = BattleDamageClass.STATUS,
			power = null,
			statusApplications = listOf(BattleStatusApplication(BattleMajorStatus.SLEEP, BattleEffectTarget.TARGET, 100)),
		)
		val resolved = BattleEngine().let { engine ->
			engine.resolveTurn(
				engine.start(
					initialState(
						first = participant("attacker", 100, skill = sleepSkill),
						second = participant(
							"holder",
							50,
							abilityEffects = listOf(BattleAbilityEffect.SleepDurationDivisor(2)),
						),
					),
				),
				listOf(BattleAction.UseSkill("attacker", sleepSkill.skillId, "holder")),
				ScriptedBattleRandom(listOf(2)),
			)
		}

		assertEquals(BattleMajorStatus.SLEEP, resolved.participant("holder")?.majorStatus)
		assertEquals(1, resolved.participant("holder")?.sleepTurnsRemaining)
	}
}
