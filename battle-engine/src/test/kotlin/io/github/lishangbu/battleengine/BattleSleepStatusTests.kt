package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.SkillPreventionReason
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleStatusApplication
import io.github.lishangbu.battleengine.model.BattleStatusBlockReason
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 验证现代睡眠状态和场地防睡眠规则。
 *
 * 场景类型：状态机级公开规则 fixture。
 * 参考来源类型：公开规则说明。睡眠持续按“阻止行动 1..3 次”建模；切换不重置睡眠计数；
 * 电气场地只阻止当前上场且接地的成员新获得睡眠。
 * 验证重点：睡眠不消耗 PP、持续计数可复盘、电气场地阻止睡眠且不消费睡眠持续随机数。
 */
class BattleSleepStatusTests {
	private val engine = BattleEngine()

	@Test
	fun `sleep consumes deterministic duration and prevents actions without pp loss`() {
		val fixture = publicBattleRuleFixture(
			name = "sleep-prevents-two-actions-from-scripted-duration",
			sourceUrls = listOf(
				"https://bulbapedia.bulbagarden.net/wiki/Sleep_(status_condition)",
			),
			inputSummary = "睡眠技能命中较慢目标，固定随机数让睡眠阻止行动次数为 2。",
			expectedSummary = "目标本回合和下一回合各被睡眠阻止一次，期间不消耗 PP；计数归零后状态解除，之后可正常行动。",
		)
		val sleepSkill = damagingSkill(
			damageClass = BattleDamageClass.STATUS,
			statusApplications = listOf(
				BattleStatusApplication(
					status = BattleMajorStatus.SLEEP,
					target = BattleEffectTarget.TARGET,
					chancePercent = 100,
				),
			),
		)
		val state = engine.start(
			initialState(
				first = participant("sleep-user", speed = 100, skill = sleepSkill),
				second = participant("target", speed = 50),
			),
		)
		val firstRandom = ScriptedBattleRandom(listOf(1))

		val afterFirst = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("sleep-user", skillId = 1, targetActorId = "target"),
				BattleAction.UseSkill("target", skillId = 1, targetActorId = "sleep-user"),
			),
			firstRandom,
		)
		val afterSecond = engine.resolveTurn(
			afterFirst,
			listOf(BattleAction.UseSkill("target", skillId = 1, targetActorId = "sleep-user")),
			ScriptedBattleRandom(emptyList()),
		)
		val afterThird = engine.resolveTurn(
			afterSecond,
			listOf(BattleAction.UseSkill("target", skillId = 1, targetActorId = "sleep-user")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		fixture.assertNamed("sleep-prevents-two-actions-from-scripted-duration")
		assertEquals(listOf("sleep duration for 1"), firstRandom.consumedReasons())
		assertEquals(BattleMajorStatus.SLEEP, afterFirst.participant("target")?.majorStatus)
		assertEquals(1, afterFirst.participant("target")?.sleepTurnsRemaining)
		assertEquals(35, afterFirst.participant("target")?.skillSlot(1)?.remainingPp)
		assertEquals(null, afterSecond.participant("target")?.majorStatus)
		assertEquals(0, afterSecond.participant("target")?.sleepTurnsRemaining)
		assertEquals(35, afterSecond.participant("target")?.skillSlot(1)?.remainingPp)
		assertEquals(72, afterThird.participant("sleep-user")?.currentHp)
		assertEquals(34, afterThird.participant("target")?.skillSlot(1)?.remainingPp)
		assertEquals(2, afterSecond.events.filterIsInstance<BattleEvent.SkillPrevented>().filter { it.reason == SkillPreventionReason.SLEEP }.size)
		assertEquals("target", afterSecond.events.filterIsInstance<BattleEvent.StatusCleared>().single().actorId)
	}

	@Test
	fun `electric terrain blocks new sleep without consuming duration random`() {
		val fixture = publicBattleRuleFixture(
			name = "electric-terrain-blocks-new-sleep",
			sourceUrls = listOf(
				"https://bulbapedia.bulbagarden.net/wiki/Electric_Terrain_(move)",
			),
			inputSummary = "电气场地存在时，上场目标被睡眠技能命中。",
			expectedSummary = "目标不会获得睡眠状态，事件流记录场地阻止；睡眠持续回合随机数不被消费。",
		)
		val sleepSkill = damagingSkill(
			damageClass = BattleDamageClass.STATUS,
			statusApplications = listOf(
				BattleStatusApplication(
					status = BattleMajorStatus.SLEEP,
					target = BattleEffectTarget.TARGET,
					chancePercent = 100,
				),
			),
		)
		val state = engine.start(
			initialState(
				first = participant("sleep-user", speed = 100, skill = sleepSkill),
				second = participant("target", speed = 50),
				environment = BattleEnvironment(terrain = BattleTerrain.ELECTRIC),
			),
		)
		val random = ScriptedBattleRandom(emptyList())

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("sleep-user", skillId = 1, targetActorId = "target")),
			random,
		)

		fixture.assertNamed("electric-terrain-blocks-new-sleep")
		assertEquals(null, resolved.participant("target")?.majorStatus)
		assertEquals(emptyList(), random.consumedReasons())
		assertTrue(resolved.events.filterIsInstance<BattleEvent.StatusApplied>().isEmpty())
		val blocked = resolved.events.filterIsInstance<BattleEvent.StatusApplicationBlocked>().single()
		assertEquals("target", blocked.targetActorId)
		assertEquals(BattleMajorStatus.SLEEP, blocked.status)
		assertEquals(BattleStatusBlockReason.TERRAIN, blocked.reason)
	}

}
