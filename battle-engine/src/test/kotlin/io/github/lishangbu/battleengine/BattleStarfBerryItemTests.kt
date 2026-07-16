package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleFixedDamage
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/** 验证星桃果只从未到上限的战斗能力中随机提升一项。 */
class BattleStarfBerryItemTests {
	@Test
	fun `starf berry randomly raises an eligible stat by two stages`() {
		val effect = BattleItemEffect.LowHpRandomStatStageBoost(
			stats = setOf(BattleStat.ATTACK, BattleStat.DEFENSE, BattleStat.SPECIAL_ATTACK, BattleStat.SPECIAL_DEFENSE, BattleStat.SPEED),
			stageDelta = 2,
		)
		val holder = participant("holder", 50, currentHp = 25, itemId = 207, itemEffects = listOf(effect))
			.copy(statStages = mapOf(BattleStat.ATTACK to 6))
		val random = ScriptedBattleRandom(listOf(1))
		val skill = damagingSkill(skillId = 311, power = null, fixedDamage = BattleFixedDamage.FixedAmount(1))

		val resolved = BattleEngine().resolveTurn(
			BattleEngine().start(initialState(first = participant("attacker", 100, skill = skill), second = holder)),
			listOf(BattleAction.UseSkill("attacker", 311, "holder")),
			random,
		)

		assertEquals(2, resolved.participant("holder")?.statStage(BattleStat.SPECIAL_ATTACK))
		assertEquals(null, resolved.participant("holder")?.itemId)
		assertEquals(listOf("low hp random stat boost for holder"), random.consumedReasons())
	}
}
