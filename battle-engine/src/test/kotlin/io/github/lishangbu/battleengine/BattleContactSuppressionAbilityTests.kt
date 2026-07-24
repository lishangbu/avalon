package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 验证远隔等特性取消技能接触判定后的公开行为。 */
class BattleContactSuppressionAbilityTests {
	@Test
	fun `contact suppression prevents defender contact retaliation`() {
		val skill = damagingSkill(makesContact = true)
		val engine = BattleEngine()
		val state = engine.start(
			initialState(
				first = participant(
					"long-reach-user",
					speed = 100,
					skill = skill,
					abilityEffects = listOf(BattleAbilityEffect.ContactSuppression()),
				),
				second = participant(
					"rough-skin-target",
					speed = 50,
					abilityEffects = listOf(BattleAbilityEffect.ContactDamageToAttacker(8)),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("long-reach-user", skill.skillId, "rough-skin-target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		assertEquals(100, resolved.participant("long-reach-user")?.currentHp)
		assertTrue(resolved.events.filterIsInstance<BattleEvent.RecoilDamageApplied>().isEmpty())
	}
}
