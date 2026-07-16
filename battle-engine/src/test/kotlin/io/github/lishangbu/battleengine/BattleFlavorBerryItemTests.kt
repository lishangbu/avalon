package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleFixedDamage
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/** 验证混乱树果按性格降低能力判断讨厌口味，并共享战斗随机轨迹。 */
class BattleFlavorBerryItemTests {
	@Test
	fun `flavor berry heals then confuses holder that dislikes its flavor`() {
		val holder = participant(
			actorId = "holder", speed = 50, currentHp = 25, itemId = 159,
			itemEffects = listOf(flavorBerry(BattleStat.ATTACK)),
		).copy(natureDecreasedStat = BattleStat.ATTACK)
		val random = ScriptedBattleRandom(listOf(2))

		val resolved = resolveBerryTrigger(holder, random)

		assertEquals(57, resolved.participant("holder")?.currentHp)
		assertEquals(4, resolved.participant("holder")?.confusionTurnsRemaining)
		assertEquals(null, resolved.participant("holder")?.itemId)
		assertEquals(listOf("flavor berry confusion duration for holder"), random.consumedReasons())
	}

	@Test
	fun `flavor berry does not confuse holder with another decreased stat`() {
		val holder = participant(
			actorId = "holder", speed = 50, currentHp = 25, itemId = 159,
			itemEffects = listOf(flavorBerry(BattleStat.ATTACK)),
		).copy(natureDecreasedStat = BattleStat.DEFENSE)
		val random = ScriptedBattleRandom(emptyList())

		val resolved = resolveBerryTrigger(holder, random)

		assertEquals(57, resolved.participant("holder")?.currentHp)
		assertEquals(0, resolved.participant("holder")?.confusionTurnsRemaining)
		assertEquals(emptyList(), random.consumedReasons())
	}

	private fun resolveBerryTrigger(holder: io.github.lishangbu.battleengine.model.BattleParticipant, random: ScriptedBattleRandom) =
		BattleEngine().resolveTurn(
			BattleEngine().start(
				initialState(first = participant("attacker", 100, skill = fixedDamageSkill()), second = holder),
			),
			listOf(BattleAction.UseSkill("attacker", 301, "holder")),
			random,
		)

	private fun flavorBerry(dislikedStat: BattleStat) = BattleItemEffect.LowHpHeal(
		triggerHpNumerator = 1,
		triggerHpDenominator = 4,
		healDenominator = 3,
		confusesIfNatureDecreases = dislikedStat,
	)

	private fun fixedDamageSkill() = damagingSkill(
		skillId = 301,
		name = "固定伤害",
		power = null,
		fixedDamage = BattleFixedDamage.FixedAmount(1),
	)
}
