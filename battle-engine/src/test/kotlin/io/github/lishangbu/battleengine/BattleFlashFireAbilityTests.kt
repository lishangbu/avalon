package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BattleFlashFireAbilityTests {
	@Test
	fun `flash fire absorbs fire damage and boosts later fire skills`() {
		val fireElementId = 10L
		val fireSkill = damagingSkill(skillId = 1071, elementId = fireElementId, power = 40)
		val effect = BattleAbilityEffect.ElementSkillAbsorbDamageBoost(fireElementId, 1.5)
		val engine = BattleEngine()
		val started = engine.start(
			initialState(
				first = participant("attacker", 100, skill = fireSkill),
				second = participant("holder", 50, skill = fireSkill, abilityEffects = listOf(effect)),
			),
		)
		val absorbed = engine.resolveTurn(
			started,
			listOf(BattleAction.UseSkill("attacker", fireSkill.skillId, "holder")),
			ScriptedBattleRandom(emptyList()),
		)
		val retaliated = engine.resolveTurn(
			absorbed,
			listOf(BattleAction.UseSkill("holder", fireSkill.skillId, "attacker")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		assertEquals(100, absorbed.participant("holder")?.currentHp)
		assertTrue(retaliated.events.filterIsInstance<BattleEvent.DamageApplied>().last().amount > 19)
	}
}
