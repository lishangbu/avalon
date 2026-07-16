package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BattleAccuracyAbilityTests {
	@Test
	fun `user accuracy multiplier changes normal hit check`() {
		val skill = damagingSkill(accuracy = 70)
		val resolved = resolve(
			actor = participant("actor", 100, skill = skill).copy(
				abilityEffects = listOf(BattleAbilityEffect.AccuracyMultiplier(1.3)),
			),
			target = participant("target", 50),
			random = ScriptedBattleRandom(listOf(84, 1, 15)),
		)

		assertTrue(resolved.events.filterIsInstance<BattleEvent.SkillMissed>().isEmpty())
	}

	@Test
	fun `always hit skips accuracy random when held by either participant`() {
		val skill = damagingSkill(accuracy = 1)
		val random = ScriptedBattleRandom(listOf(1, 15))
		val resolved = resolve(
			actor = participant("actor", 100, skill = skill),
			target = participant("target", 50).copy(abilityEffects = listOf(BattleAbilityEffect.AlwaysHit())),
			random = random,
		)

		assertTrue(resolved.events.filterIsInstance<BattleEvent.SkillMissed>().isEmpty())
		assertTrue(random.consumedReasons().none { it.startsWith("accuracy") })
	}

	@Test
	fun `wonder skin caps status skill accuracy`() {
		val skill = damagingSkill(damageClass = BattleDamageClass.STATUS, power = null, accuracy = 100)
		val resolved = resolve(
			actor = participant("actor", 100, skill = skill),
			target = participant("target", 50).copy(
				abilityEffects = listOf(BattleAbilityEffect.StatusSkillAccuracyCap(50)),
			),
			random = ScriptedBattleRandom(listOf(50)),
		)

		assertEquals(51, resolved.events.filterIsInstance<BattleEvent.SkillMissed>().single().accuracyRoll)
	}

	private fun resolve(
		actor: io.github.lishangbu.battleengine.model.BattleParticipant,
		target: io.github.lishangbu.battleengine.model.BattleParticipant,
		random: ScriptedBattleRandom,
	): io.github.lishangbu.battleengine.model.BattleState {
		val engine = BattleEngine()
		return engine.resolveTurn(
			engine.start(initialState(first = actor, second = target)),
			listOf(BattleAction.UseSkill("actor", actor.skillSlots.single().skillId, "target")),
			random,
		)
	}
}
