package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleStatStageEffect
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 验证密探斗篷阻止伤害技能对持有者造成的追加效果。 */
class BattleCovertCloakItemTests {
	@Test
	fun `covert cloak blocks damaging move stat drop but not damage`() {
		val skill = damagingSkill(
			skillId = 341,
			statStageEffects = listOf(BattleStatStageEffect(BattleStat.DEFENSE, BattleEffectTarget.TARGET, -1, 100)),
		)
		val holder = participant(
			"holder", 50, itemId = 1885,
			itemEffects = listOf(BattleItemEffect.DamagingSkillSecondaryEffectImmunity()),
		)

		val resolved = BattleEngine().resolveTurn(
			BattleEngine().start(initialState(first = participant("attacker", 100, skill = skill), second = holder)),
			listOf(BattleAction.UseSkill("attacker", 341, "holder")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		assertTrue(requireNotNull(resolved.participant("holder")).currentHp < 100)
		assertEquals(0, resolved.participant("holder")?.statStage(BattleStat.DEFENSE))
		assertEquals(1885, resolved.participant("holder")?.itemId)
	}
}
