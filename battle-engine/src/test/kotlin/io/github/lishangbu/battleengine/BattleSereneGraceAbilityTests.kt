package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleStatusApplication
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

class BattleSereneGraceAbilityTests {
	@Test
	fun `serene grace doubles damaging skill secondary effect chance`() {
		val skill = damagingSkill(skillId = 941, power = 40).copy(
			statusApplications = listOf(
				BattleStatusApplication(BattleMajorStatus.BURN, BattleEffectTarget.TARGET, 30),
			),
		)

		assertEquals(0, statusEvents(skill, emptyList()))
		assertEquals(1, statusEvents(skill, listOf(BattleAbilityEffect.SecondaryEffectChanceMultiplier(2.0))))
	}

	private fun statusEvents(skill: io.github.lishangbu.battleengine.model.BattleSkillSlot, effects: List<BattleAbilityEffect>): Int {
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("attacker", 100, skill = skill, abilityEffects = effects),
					second = participant("defender", 50),
				),
			),
			listOf(BattleAction.UseSkill("attacker", skill.skillId, "defender")),
			ScriptedBattleRandom(listOf(1, 15, 50)),
		)
		return resolved.events.filterIsInstance<BattleEvent.StatusApplied>().size
	}
}
