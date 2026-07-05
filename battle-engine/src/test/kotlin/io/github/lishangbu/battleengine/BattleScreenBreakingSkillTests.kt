package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleRuleSnapshot
import io.github.lishangbu.battleengine.model.BattleSideDamageReduction
import io.github.lishangbu.battleengine.model.BattleSideDamageReductionKind
import io.github.lishangbu.battleengine.model.ElementEffectivenessChart
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 验证命中后、伤害计算前击破目标侧屏障的技能族。
 *
 * 场景类型：防守方一侧屏障清除时机 场景。
 * 参考来源类型：公开成熟模拟器实现和公开规则说明。现代规则要求这类技能在没有未命中、未被保护且目标没有属性免疫时，
 * 先结束目标侧的物理屏障、特殊屏障和全伤害屏障，再进入本次伤害公式；因此本次伤害不能再读取刚刚被清除的屏障倍率。
 * 这里把属性免疫场景一起固定，是为了避免把“没有命中成功的目标侧屏障”错误清掉。
 */
class BattleScreenBreakingSkillTests {
	private val engine = BattleEngine()

	@Test
	fun `screen breaking damage skill removes target side screens before damage`() {
		val scenario = publicBattleRuleScenario(
			name = "screen-breaking-damage-skill-removes-target-side-screens-before-damage",
			inputSummary = "目标侧同时存在物理、特殊和全伤害屏障，随后受到一个命中且声明击破屏障的普通物理伤害技能。",
			expectedSummary = "三类屏障在伤害事件前被移除，本次伤害按无屏障倍率计算，目标侧屏障列表清空。",
		)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = screenBreakingSkill()),
				second = participant("target", speed = 50),
				secondSideDamageReductions = listOf(
					BattleSideDamageReduction(BattleSideDamageReductionKind.PHYSICAL, turnsRemaining = 3),
					BattleSideDamageReduction(BattleSideDamageReductionKind.SPECIAL, turnsRemaining = 3),
					BattleSideDamageReduction(BattleSideDamageReductionKind.ALL_STANDARD_DAMAGE, turnsRemaining = 3),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 9280, targetActorId = "target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val removed = resolved.events.filterIsInstance<BattleEvent.SideDamageReductionsRemoved>().single()
		val damage = resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single()

		scenario.assertNamed("screen-breaking-damage-skill-removes-target-side-screens-before-damage")
		assertEquals(
			listOf(
				BattleSideDamageReductionKind.PHYSICAL,
				BattleSideDamageReductionKind.SPECIAL,
				BattleSideDamageReductionKind.ALL_STANDARD_DAMAGE,
			),
			removed.removedKinds,
		)
		assertEquals(emptyList(), resolved.sideOf("target")?.damageReductions)
		assertEquals(28, damage.amount)
		assertTrue(resolved.events.indexOf(removed) < resolved.events.indexOf(damage))
	}

	@Test
	fun `screen breaking damage skill keeps screens when target is immune`() {
		val scenario = publicBattleRuleScenario(
			name = "screen-breaking-damage-skill-keeps-screens-when-target-is-immune",
			inputSummary = "目标侧存在物理屏障，但属性克制表声明本次伤害对目标完全无效。",
			expectedSummary = "技能只记录 0 伤害免疫事件，不移除目标侧屏障，也不消费要害或伤害随机数。",
		)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, elementId = 1, skill = screenBreakingSkill(elementId = 1)),
				second = participant("target", speed = 50, elementId = 2),
				secondSideDamageReductions = listOf(
					BattleSideDamageReduction(BattleSideDamageReductionKind.PHYSICAL, turnsRemaining = 3),
				),
				rules = BattleRuleSnapshot(
					elementChart = ElementEffectivenessChart(mapOf(1L to mapOf(2L to 0.0))),
				),
			),
		)
		val random = ScriptedBattleRandom(emptyList())

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 9280, targetActorId = "target")),
			random,
		)

		scenario.assertNamed("screen-breaking-damage-skill-keeps-screens-when-target-is-immune")
		assertEquals(0, resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount)
		assertEquals(
			listOf(BattleSideDamageReduction(BattleSideDamageReductionKind.PHYSICAL, turnsRemaining = 2)),
			resolved.sideOf("target")?.damageReductions,
		)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SideDamageReductionsRemoved>())
		assertEquals(emptyList(), random.consumedReasons())
	}

	private fun screenBreakingSkill(elementId: Long = 1) =
		damagingSkill(
			skillId = 9280,
			name = "击破屏障测试",
			elementId = elementId,
			power = 40,
			breaksTargetSideDamageReductions = true,
		)
}
