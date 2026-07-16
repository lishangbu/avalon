package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BattleSlowStartAbilityTests {
	@Test
	fun `slow start halves attack and speed during the first five active turns`() {
		val effect = BattleAbilityEffect.InitialActiveTurnsStatMultiplier(
			turns = 5,
			stats = setOf(BattleStat.ATTACK, BattleStat.SPEED),
			multiplier = 0.5,
		)
		val engine = BattleEngine()
		val skill = damagingSkill(skillId = 1001, power = 40)
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("slow-start-user", 100, skill = skill, abilityEffects = listOf(effect)),
					second = participant("opponent", 60),
				),
			),
			listOf(
				BattleAction.UseSkill("slow-start-user", skill.skillId, "opponent"),
				BattleAction.UseSkill("opponent", 1, "slow-start-user"),
			),
			ScriptedBattleRandom(listOf(1, 15, 1, 15)),
		)
		val events = resolved.events

		assertEquals("opponent", events.filterIsInstance<BattleEvent.SkillUsed>().first().actorId)
		assertTrue(events.filterIsInstance<BattleEvent.DamageApplied>().single { it.actorId == "slow-start-user" }.amount < 28)
	}
}
