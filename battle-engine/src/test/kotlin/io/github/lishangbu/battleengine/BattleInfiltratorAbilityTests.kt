package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleSideDamageReduction
import io.github.lishangbu.battleengine.model.BattleSideDamageReductionKind
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BattleInfiltratorAbilityTests {
	@Test
	fun `infiltrator bypasses opposing substitute and side damage reduction`() {
		val neutral = resolve(emptyList(), substituteHp = 0, withScreen = true)
		val infiltrator = resolve(
			listOf(BattleAbilityEffect.OpponentBarrierBypass()),
			substituteHp = 25,
			withScreen = true,
		)

		assertEquals(25, infiltrator.participant("defender")?.substituteHp)
		assertTrue(requireNotNull(infiltrator.participant("defender")).currentHp < 100)
		assertTrue(
			infiltrator.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount >
				neutral.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount,
		)
	}

	private fun resolve(
		effects: List<BattleAbilityEffect>,
		substituteHp: Int,
		withScreen: Boolean,
	): io.github.lishangbu.battleengine.model.BattleState {
		val skill = damagingSkill(skillId = 921, power = 80)
		val engine = BattleEngine()
		return engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("attacker", 100, skill = skill, abilityEffects = effects),
					second = participant("defender", 50).copy(substituteHp = substituteHp),
					secondSideDamageReductions = if (withScreen) listOf(
						BattleSideDamageReduction(BattleSideDamageReductionKind.PHYSICAL, 5),
					) else emptyList(),
				),
			),
			listOf(BattleAction.UseSkill("attacker", skill.skillId, "defender")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
	}
}
