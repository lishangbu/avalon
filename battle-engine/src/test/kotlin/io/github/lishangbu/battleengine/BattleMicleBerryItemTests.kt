package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleFixedDamage
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 验证奇秘果在低体力时保存一次性下一技能命中修正。 */
class BattleMicleBerryItemTests {
	@Test
	fun `micle berry boosts the next skill accuracy after low hp trigger`() {
		val fixedSkill = damagingSkill(skillId = 321, power = null, fixedDamage = BattleFixedDamage.FixedAmount(1))
		val inaccurateSkill = damagingSkill(skillId = 322, accuracy = 60)
		val holder = participant(
			"holder", 50, currentHp = 25, skill = inaccurateSkill, itemId = 209,
			itemEffects = listOf(BattleItemEffect.LowHpNextSkillAccuracyBoost(1.2)),
		)
		val random = ScriptedBattleRandom(listOf(65, 1, 15))

		val resolved = BattleEngine().resolveTurn(
			BattleEngine().start(
				initialState(first = participant("attacker", 100, skill = fixedSkill), second = holder),
			),
			listOf(
				BattleAction.UseSkill("attacker", 321, "holder"),
				BattleAction.UseSkill("holder", 322, "attacker"),
			),
			random,
		)

		assertTrue(requireNotNull(resolved.participant("attacker")).currentHp < 100)
		assertEquals(null, resolved.participant("holder")?.itemId)
		assertEquals(1.0, resolved.participant("holder")?.nextSkillAccuracyMultiplier)
	}
}
