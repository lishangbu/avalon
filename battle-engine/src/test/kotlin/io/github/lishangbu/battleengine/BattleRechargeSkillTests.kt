package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.SkillPreventionReason
import io.github.lishangbu.battleengine.model.BattleRuleSnapshot
import io.github.lishangbu.battleengine.model.ElementEffectivenessChart
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证成功后休整类技能的基础结算。
 *
 * 场景类型：技能执行流程 场景。
 * 参考来源类型：公开成熟模拟器实现和公开规则说明。现代休整类技能成功造成实际伤害后，会让使用者下一次技能行动
 * 前强制休整一次；未造成实际伤害时不会进入休整。
 * 验证重点：休整只在扣除目标 HP 后写入，下一次技能行动会被阻止但不扣 PP，也不会执行命中、伤害或附加效果。
 */
class BattleRechargeSkillTests {
	private val engine = BattleEngine()

	@Test
	fun `recharge skill prevents next turn action without pp loss`() {
		val scenario = publicBattleRuleScenario(
			name = "recharge-skill-prevents-next-turn-action-without-pp-loss",
			inputSummary = "使用者用成功后休整技能命中目标，下一回合再次提交技能行动。",
			expectedSummary = "首次命中造成实际伤害并写入一次休整；下一次技能行动被休整阻止，不消耗 PP，也不再次造成伤害。",
		)
		val skill = damagingSkill(name = "休整测试", rechargesAfterUse = true)
		val state = engine.start(
			initialState(
				first = participant("recharge-user", speed = 100, skill = skill),
				second = participant("target", speed = 50),
			),
		)

		val afterFirst = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("recharge-user", skillId = 1, targetActorId = "target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val afterSecond = engine.resolveTurn(
			afterFirst,
			listOf(BattleAction.UseSkill("recharge-user", skillId = 1, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("recharge-skill-prevents-next-turn-action-without-pp-loss")
		val rechargeStarted = afterSecond.events.filterIsInstance<BattleEvent.RechargeStarted>().single()
		assertEquals("recharge-user", rechargeStarted.actorId)
		assertEquals(1, rechargeStarted.turnsRemainingAfterCurrent)
		val prevented = afterSecond.events.filterIsInstance<BattleEvent.SkillPrevented>().filter { it.reason == SkillPreventionReason.RECHARGE }.single()
		assertEquals("recharge-user", prevented.actorId)
		assertEquals(1, prevented.turnsRemainingBefore)
		assertEquals(0, afterSecond.participant("recharge-user")?.rechargeTurnsRemaining)
		assertEquals(34, afterSecond.participant("recharge-user")?.skillSlot(1)?.remainingPp)
		assertEquals(72, afterSecond.participant("target")?.currentHp)
		assertEquals(1, afterSecond.events.filterIsInstance<BattleEvent.SkillUsed>().size)
		assertEquals(1, afterSecond.events.filterIsInstance<BattleEvent.DamageApplied>().size)
	}

	@Test
	fun `recharge skill does not start recharge when target is immune`() {
		val scenario = publicBattleRuleScenario(
			name = "recharge-skill-does-not-start-when-target-is-immune",
			inputSummary = "成功后休整技能命中检查通过，但属性相性让目标完全免疫本次伤害。",
			expectedSummary = "本次行动只产生 0 伤害事件，不写入下一回合休整状态，也不消费普通伤害随机数。",
		)
		val skill = damagingSkill(name = "休整免疫测试", elementId = 1, rechargesAfterUse = true)
		val state = engine.start(
			initialState(
				first = participant("recharge-user", speed = 100, elementId = 1, skill = skill),
				second = participant("immune-target", speed = 50, elementId = 2),
				rules = BattleRuleSnapshot(
					elementChart = ElementEffectivenessChart(mapOf(1L to mapOf(2L to 0.0))),
				),
			),
		)
		val random = ScriptedBattleRandom(emptyList())

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("recharge-user", skillId = 1, targetActorId = "immune-target")),
			random,
		)

		scenario.assertNamed("recharge-skill-does-not-start-when-target-is-immune")
		val damage = resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single()
		assertEquals(0, damage.amount)
		assertEquals(0.0, damage.effectiveness)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.RechargeStarted>())
		assertEquals(0, resolved.participant("recharge-user")?.rechargeTurnsRemaining)
		assertEquals(34, resolved.participant("recharge-user")?.skillSlot(1)?.remainingPp)
		assertEquals(emptyList(), random.consumedReasons())
	}
}
