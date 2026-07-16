package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleStatStageEffect
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BattleForcedSwitchItemTests {
	@Test
	fun `eject button switches damaged holder to its bench`() {
		val resolved = resolveDamageItem(
			target = participant(
				"holder", 50, itemId = 590,
				itemEffects = listOf(BattleItemEffect.DamagedForceSelfSwitch()),
			),
			firstBench = emptyList(),
			secondBench = listOf(participant("holder-bench", 40)),
		)

		assertEquals(listOf("holder-bench"), resolved.sideOf("holder-bench")?.activeActorIds)
		assertNull(resolved.participant("holder")?.itemId)
		assertEquals("holder", resolved.events.filterIsInstance<BattleEvent.ItemForcedSwitchSelected>().single().targetActorId)
	}

	@Test
	fun `red card switches attacker to its bench`() {
		val resolved = resolveDamageItem(
			target = participant(
				"holder", 50, itemId = 585,
				itemEffects = listOf(BattleItemEffect.DamagedForceAttackerSwitch()),
			),
			firstBench = listOf(participant("attacker-bench", 40)),
			secondBench = emptyList(),
		)

		assertEquals(listOf("attacker-bench"), resolved.sideOf("attacker-bench")?.activeActorIds)
		assertNull(resolved.participant("holder")?.itemId)
		assertEquals("attacker", resolved.events.filterIsInstance<BattleEvent.ItemForcedSwitchSelected>().single().targetActorId)
	}

	@Test
	fun `eject pack switches holder after actual stat reduction`() {
		val skill = damagingSkill(
			damageClass = BattleDamageClass.STATUS,
			power = null,
			statStageEffects = listOf(BattleStatStageEffect(BattleStat.DEFENSE, BattleEffectTarget.TARGET, -1, 100)),
		)
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("attacker", 100, skill = skill),
					second = participant(
						"holder", 50, itemId = 1177,
						itemEffects = listOf(BattleItemEffect.NegativeStatStageForceSelfSwitch()),
					),
					secondBench = listOf(participant("holder-bench", 40)),
				),
			),
			listOf(BattleAction.UseSkill("attacker", skill.skillId, "holder")),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals(listOf("holder-bench"), resolved.sideOf("holder-bench")?.activeActorIds)
		assertEquals(
			-1,
			resolved.events.filterIsInstance<BattleEvent.StatStageChanged>()
				.first { it.targetActorId == "holder" && it.stat == BattleStat.DEFENSE }
				.delta,
		)
		assertNull(resolved.participant("holder")?.itemId)
	}

	private fun resolveDamageItem(
		target: io.github.lishangbu.battleengine.model.BattleParticipant,
		firstBench: List<io.github.lishangbu.battleengine.model.BattleParticipant>,
		secondBench: List<io.github.lishangbu.battleengine.model.BattleParticipant>,
	): io.github.lishangbu.battleengine.model.BattleState {
		val engine = BattleEngine()
		return engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("attacker", 100),
					second = target,
					firstBench = firstBench,
					secondBench = secondBench,
				),
			),
			listOf(BattleAction.UseSkill("attacker", 1, "holder")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
	}
}
