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
import kotlin.test.assertTrue

class BattleSheerForceAbilityTests {
	@Test
	fun `sheer force boosts eligible damage and suppresses secondary effects`() {
		val skill = damagingSkill(skillId = 1061, power = 40).copy(
			statusApplications = listOf(
				BattleStatusApplication(BattleMajorStatus.BURN, BattleEffectTarget.TARGET, 100),
			),
		)
		val neutral = resolve(skill, emptyList())
		val sheerForce = resolve(
			skill,
			listOf(BattleAbilityEffect.SecondaryEffectsSuppressedDamageBoost(1.3)),
		)

		assertTrue(
			sheerForce.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount >
				neutral.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount,
		)
		assertEquals(null, sheerForce.participant("defender")?.majorStatus)
	}

	private fun resolve(
		skill: io.github.lishangbu.battleengine.model.BattleSkillSlot,
		effects: List<BattleAbilityEffect>,
	): io.github.lishangbu.battleengine.model.BattleState {
		val engine = BattleEngine()
		return engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("attacker", 100, skill = skill, abilityEffects = effects),
					second = participant("defender", 50),
				),
			),
			listOf(BattleAction.UseSkill("attacker", skill.skillId, "defender")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
	}
}
