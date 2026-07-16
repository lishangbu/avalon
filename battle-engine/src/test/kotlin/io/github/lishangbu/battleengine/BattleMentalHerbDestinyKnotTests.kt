package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleVolatileStatus
import io.github.lishangbu.battleengine.model.BattleVolatileStatusApplication
import io.github.lishangbu.battleengine.model.SkillPreventionReason
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BattleMentalHerbDestinyKnotTests {
	@Test
	fun `mental herb cures supported mental volatile status after application`() {
		val skill = statusSkill(BattleVolatileStatus.TAUNT)
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("attacker", 100, skill = skill),
					second = participant(
						"holder", 50, itemId = 196,
						itemEffects = listOf(
							BattleItemEffect.VolatileStatusCure(
								setOf(
									BattleVolatileStatus.HEAL_BLOCK,
									BattleVolatileStatus.TAUNT,
									BattleVolatileStatus.DISABLE,
									BattleVolatileStatus.TORMENT,
									BattleVolatileStatus.INFATUATION,
								),
							),
						),
					),
				),
			),
			listOf(BattleAction.UseSkill("attacker", skill.skillId, "holder")),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals(0, resolved.participant("holder")?.tauntTurnsRemaining)
		assertNull(resolved.participant("holder")?.itemId)
		assertEquals(BattleVolatileStatus.TAUNT, resolved.events.filterIsInstance<BattleEvent.VolatileStatusCleared>().single().status)
	}

	@Test
	fun `destiny knot reflects infatuation and reflected source can be immobilized`() {
		val infatuation = statusSkill(BattleVolatileStatus.INFATUATION)
		val engine = BattleEngine()
		val afterReflection = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("source", 100, skill = infatuation),
					second = participant(
						"holder", 50, itemId = 257,
						itemEffects = listOf(BattleItemEffect.InfatuationReflectToSource()),
					),
				),
			),
			listOf(BattleAction.UseSkill("source", infatuation.skillId, "holder")),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals("source", afterReflection.participant("holder")?.infatuatedByActorId)
		assertEquals("holder", afterReflection.participant("source")?.infatuatedByActorId)
		assertEquals(257, afterReflection.participant("holder")?.itemId)

		val blocked = engine.resolveTurn(
			afterReflection,
			listOf(BattleAction.UseSkill("source", infatuation.skillId, "holder")),
			ScriptedBattleRandom(listOf(0)),
		)
		val prevented = blocked.events.filterIsInstance<BattleEvent.SkillPrevented>().last()
		assertEquals(SkillPreventionReason.VOLATILE_STATUS, prevented.reason)
		assertEquals(BattleVolatileStatus.INFATUATION, prevented.status)
	}

	private fun statusSkill(status: BattleVolatileStatus) = damagingSkill(
		skillId = 213,
		damageClass = BattleDamageClass.STATUS,
		power = null,
		volatileStatusApplications = listOf(BattleVolatileStatusApplication(status, BattleEffectTarget.TARGET, 100)),
	)
}
