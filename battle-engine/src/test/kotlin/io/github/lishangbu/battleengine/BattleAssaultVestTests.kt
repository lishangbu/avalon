package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/** 验证突击背心的特防倍率与变化技能选择限制共享同一携带道具生命周期。 */
class BattleAssaultVestTests {
	private val engine = BattleEngine()
	private val validator = BattleActionValidator()

	@Test
	fun `assault vest multiplies special defense by one and a half`() {
		val specialSkill = damagingSkill(damageClass = BattleDamageClass.SPECIAL)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = specialSkill),
				second = participant(
					"vest-holder",
					speed = 50,
					itemId = 640,
					itemEffects = listOf(
						BattleItemEffect.DefendingStatMultiplier(
							stats = setOf(BattleStat.SPECIAL_DEFENSE),
							multiplier = 1.5,
						),
						BattleItemEffect.StatusSkillRestriction(),
					),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "vest-holder")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		assertEquals(19, resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount)
		assertEquals(81, resolved.participant("vest-holder")?.currentHp)
	}

	@Test
	fun `assault vest rejects submitted status skill`() {
		val statusSkill = damagingSkill(damageClass = BattleDamageClass.STATUS)
		val state = engine.start(
			initialState(
				first = participant(
					"vest-holder",
					speed = 100,
					skill = statusSkill,
					itemId = 640,
					itemEffects = listOf(BattleItemEffect.StatusSkillRestriction()),
				).copy(skillSlots = listOf(statusSkill, damagingSkill(skillId = 2))),
				second = participant("target", speed = 50),
			),
		)

		val violations = validator.validate(
			state,
			listOf(BattleAction.UseSkill("vest-holder", skillId = 1, targetActorId = "target")),
		)

		assertEquals(listOf("item-prevents-status-skill"), violations.map { it.code })
	}
}
