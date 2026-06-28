package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证现代冰冻状态的行动前自然解冻规则。
 *
 * 场景类型：行动前钩子级公开规则 fixture。
 * 参考来源类型：公开成熟模拟器实现和公开规则说明。冰冻成员行动前先进行 20% 自然解冻判定；
 * 解冻则继续执行原技能，未解冻则本次无法行动且不消耗 PP。
 * 验证重点：自然解冻事件先于技能使用出现，未解冻分支不会清除状态，也不会进入普通技能流程。
 */
class BattleFreezeStatusTests {
	private val engine = BattleEngine()

	@Test
	fun `freeze can prevent action without pp loss`() {
		val fixture = publicBattleRuleFixture(
			name = "freeze-prevents-action-after-failed-thaw-roll",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
				"https://bulbapedia.bulbagarden.net/wiki/Freeze_(status_condition)",
			),
			inputSummary = "冰冻成员本回合选择使用普通攻击，固定随机数未触发自然解冻。",
			expectedSummary = "成员不会使用技能、不消耗 PP，冰冻状态仍然保留到后续回合。",
		)
		val state = engine.start(
			initialState(
				first = participant("frozen", speed = 50).copy(majorStatus = BattleMajorStatus.FREEZE),
				second = participant("target", speed = 40),
			),
		)
		val random = ScriptedBattleRandom(listOf(99))

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("frozen", skillId = 1, targetActorId = "target")),
			random,
		)

		fixture.assertNamed("freeze-prevents-action-after-failed-thaw-roll")
		assertEquals(listOf("freeze thaw chance for frozen"), random.consumedReasons())
		assertEquals(BattleMajorStatus.FREEZE, resolved.participant("frozen")?.majorStatus)
		assertEquals(35, resolved.participant("frozen")?.skillSlot(1)?.remainingPp)
		assertEquals(100, resolved.participant("target")?.currentHp)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillUsed>())
		val blocked = resolved.events.filterIsInstance<BattleEvent.SkillPreventedByFreeze>().single()
		assertEquals("frozen", blocked.actorId)
	}

	@Test
	fun `freeze can thaw before action and continue into normal skill flow`() {
		val fixture = publicBattleRuleFixture(
			name = "freeze-thaws-before-action-and-continues",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
				"https://bulbapedia.bulbagarden.net/wiki/Freeze_(status_condition)",
			),
			inputSummary = "冰冻成员本回合选择普通攻击，固定随机数触发自然解冻。",
			expectedSummary = "成员先解除冰冻，再使用技能并按普通伤害流程消费要害和伤害浮动随机数。",
		)
		val state = engine.start(
			initialState(
				first = participant("frozen", speed = 50).copy(majorStatus = BattleMajorStatus.FREEZE),
				second = participant("target", speed = 40),
			),
		)
		val random = ScriptedBattleRandom(listOf(0, 1, 15))

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("frozen", skillId = 1, targetActorId = "target")),
			random,
		)

		fixture.assertNamed("freeze-thaws-before-action-and-continues")
		assertEquals(
			listOf(
				"freeze thaw chance for frozen",
				"critical hit for 1",
				"damage random for 1",
			),
			random.consumedReasons(),
		)
		assertEquals(null, resolved.participant("frozen")?.majorStatus)
		assertEquals(34, resolved.participant("frozen")?.skillSlot(1)?.remainingPp)
		assertEquals(72, resolved.participant("target")?.currentHp)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillPreventedByFreeze>())
		assertEquals("frozen", resolved.events.filterIsInstance<BattleEvent.StatusCleared>().single().actorId)
		assertEquals("frozen", resolved.events.filterIsInstance<BattleEvent.SkillUsed>().single().actorId)
	}
}
