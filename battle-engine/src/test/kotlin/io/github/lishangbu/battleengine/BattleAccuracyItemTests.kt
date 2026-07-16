package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 验证携带道具对普通技能命中率的稳定倍率修正。
 *
 * 一击必杀技能继续使用独立等级公式，不读取这些倍率；普通技能在命中与闪避阶级之后应用攻击方和目标方道具倍率，
 * 最终命中率仍按整数向下取整并进入统一随机轨迹。
 */
class BattleAccuracyItemTests {
	private val engine = BattleEngine()

	@Test
	fun `accuracy boost item turns ninety accuracy roll ninety five into hit`() {
		val state = engine.start(
			initialState(
				first = participant(
					"wide-lens-user",
					speed = 100,
					skill = damagingSkill(accuracy = 90),
					itemId = 265,
					itemEffects = listOf(BattleItemEffect.AccuracyMultiplier(multiplier = 1.1)),
				),
				second = participant("target", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("wide-lens-user", skillId = 1, targetActorId = "target")),
			ScriptedBattleRandom(listOf(94, 1, 15)),
		)

		assertTrue(resolved.events.filterIsInstance<BattleEvent.SkillMissed>().isEmpty())
		assertEquals(72, resolved.participant("target")?.currentHp)
	}

	@Test
	fun `opponent accuracy reduction item turns accurate skill roll ninety five into miss`() {
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = damagingSkill(accuracy = 100)),
				second = participant(
					"bright-powder-holder",
					speed = 50,
					itemId = 213,
					itemEffects = listOf(BattleItemEffect.OpponentAccuracyMultiplier(multiplier = 0.9)),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "bright-powder-holder")),
			ScriptedBattleRandom(listOf(94)),
		)

		assertEquals(95, resolved.events.filterIsInstance<BattleEvent.SkillMissed>().single().accuracyRoll)
		assertEquals(100, resolved.participant("bright-powder-holder")?.currentHp)
	}
}
