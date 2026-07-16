package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleStatStageEffect
import io.github.lishangbu.battleengine.model.BattleStatusBlockReason
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/** 验证能力下降免疫与下降后复原道具的不同触发时机。 */
class BattleStatProtectionItemTests {
	private val statDropSkill = damagingSkill(
		skillId = 331,
		name = "降低防御",
		damageClass = BattleDamageClass.STATUS,
		power = null,
		statStageEffects = listOf(BattleStatStageEffect(BattleStat.DEFENSE, BattleEffectTarget.TARGET, -1, 100)),
	)

	@Test
	fun `clear amulet blocks opponent stat reduction and remains held`() {
		val resolved = resolveAgainst(
			participant("holder", 50, itemId = 1882, itemEffects = listOf(BattleItemEffect.OpponentStatStageReductionImmunity())),
		)

		assertEquals(0, resolved.participant("holder")?.statStage(BattleStat.DEFENSE))
		assertEquals(1882, resolved.participant("holder")?.itemId)
		assertEquals(BattleStatusBlockReason.ITEM, resolved.events.filterIsInstance<BattleEvent.StatStageChangeBlocked>().single().reason)
	}

	@Test
	fun `white herb restores all negative stages after reduction and is consumed`() {
		val holder = participant("holder", 50, itemId = 214, itemEffects = listOf(BattleItemEffect.NegativeStatStageReset()))
			.copy(statStages = mapOf(BattleStat.ATTACK to -2))

		val resolved = resolveAgainst(holder)

		assertEquals(0, resolved.participant("holder")?.statStage(BattleStat.ATTACK))
		assertEquals(0, resolved.participant("holder")?.statStage(BattleStat.DEFENSE))
		assertEquals(null, resolved.participant("holder")?.itemId)
	}

	private fun resolveAgainst(holder: io.github.lishangbu.battleengine.model.BattleParticipant) =
		BattleEngine().resolveTurn(
			BattleEngine().start(initialState(first = participant("attacker", 100, skill = statDropSkill), second = holder)),
			listOf(BattleAction.UseSkill("attacker", 331, "holder")),
			ScriptedBattleRandom(emptyList()),
		)
}
