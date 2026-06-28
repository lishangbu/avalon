package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证现代麻痹状态的行动前阻止规则。
 *
 * 场景类型：行动前钩子级公开规则 fixture。
 * 参考来源类型：公开成熟模拟器实现和公开规则说明。麻痹会把有效速度减半，并在每次行动前以 25%
 * 概率阻止技能；阻止时不消耗 PP，也不会产生技能使用事件。
 * 验证重点：麻痹随机数只在成员真正进入麻痹判定时消费，且不会因为一次阻止而清除主要异常状态。
 */
class BattleParalysisStatusTests {
	private val engine = BattleEngine()

	@Test
	fun `paralysis can fully prevent action without pp loss`() {
		val fixture = publicBattleRuleFixture(
			name = "paralysis-prevents-action-without-pp-loss",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
				"https://bulbapedia.bulbagarden.net/wiki/Paralysis_(status_condition)",
			),
			inputSummary = "麻痹成员本回合选择使用普通攻击，固定随机数触发 25% 行动阻止。",
			expectedSummary = "成员不会使用技能、不消耗 PP，麻痹状态仍然保留到后续回合。",
		)
		val state = engine.start(
			initialState(
				first = participant("paralyzed", speed = 50).copy(majorStatus = BattleMajorStatus.PARALYSIS),
				second = participant("target", speed = 40),
			),
		)
		val random = ScriptedBattleRandom(listOf(0))

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("paralyzed", skillId = 1, targetActorId = "target")),
			random,
		)

		fixture.assertNamed("paralysis-prevents-action-without-pp-loss")
		assertEquals(listOf("paralysis chance for paralyzed"), random.consumedReasons())
		assertEquals(BattleMajorStatus.PARALYSIS, resolved.participant("paralyzed")?.majorStatus)
		assertEquals(35, resolved.participant("paralyzed")?.skillSlot(1)?.remainingPp)
		assertEquals(100, resolved.participant("target")?.currentHp)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillUsed>())
		val blocked = resolved.events.filterIsInstance<BattleEvent.SkillPreventedByParalysis>().single()
		assertEquals("paralyzed", blocked.actorId)
	}

	@Test
	fun `paralysis can allow action and still consumes action check before attack randoms`() {
		val fixture = publicBattleRuleFixture(
			name = "paralysis-allows-action-after-failed-block-roll",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
				"https://bulbapedia.bulbagarden.net/wiki/Paralysis_(status_condition)",
			),
			inputSummary = "麻痹成员本回合选择普通攻击，固定随机数未触发行动阻止。",
			expectedSummary = "成员先消费麻痹判定随机数，再按普通伤害流程消耗要害和伤害浮动随机数。",
		)
		val state = engine.start(
			initialState(
				first = participant("paralyzed", speed = 50).copy(majorStatus = BattleMajorStatus.PARALYSIS),
				second = participant("target", speed = 40),
			),
		)
		val random = ScriptedBattleRandom(listOf(99, 1, 15))

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("paralyzed", skillId = 1, targetActorId = "target")),
			random,
		)

		fixture.assertNamed("paralysis-allows-action-after-failed-block-roll")
		assertEquals(
			listOf(
				"paralysis chance for paralyzed",
				"critical hit for 1",
				"damage random for 1",
			),
			random.consumedReasons(),
		)
		assertEquals(BattleMajorStatus.PARALYSIS, resolved.participant("paralyzed")?.majorStatus)
		assertEquals(34, resolved.participant("paralyzed")?.skillSlot(1)?.remainingPp)
		assertEquals(72, resolved.participant("target")?.currentHp)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillPreventedByParalysis>())
		assertEquals("paralyzed", resolved.events.filterIsInstance<BattleEvent.SkillUsed>().single().actorId)
	}
}
