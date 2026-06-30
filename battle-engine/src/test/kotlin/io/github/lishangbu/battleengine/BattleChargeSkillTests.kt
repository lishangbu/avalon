package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.SwitchPreventionReason
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleWeather
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 验证蓄力后发动类技能的基础结算。
 *
 * 场景类型：技能执行流程 场景。
 * 参考来源类型：公开成熟模拟器实现和公开规则说明。现代蓄力类技能首次使用时消耗 PP 并进入等待状态，后续行动
 * 自动释放原技能；释放阶段不会再次消耗 PP，也不会重新选择技能。
 * 验证重点：首回合只产生蓄力事件不造成伤害，第二回合自动释放并进入普通命中和伤害流程，主动替换请求会被阻止。
 */
class BattleChargeSkillTests {
	private val engine = BattleEngine()

	@Test
	fun `charge skill spends first turn charging then releases without extra pp`() {
		val scenario = publicBattleRuleScenario(
			name = "charge-skill-releases-next-turn-without-extra-pp",
			inputSummary = "使用者使用需要蓄力的特殊伤害技能，下一回合未提交行动。",
			expectedSummary = "首回合只进入蓄力并扣一次 PP；下一回合自动释放技能造成伤害，不再次扣 PP。",
		)
		val skill = damagingSkill(name = "蓄力测试", chargesBeforeUse = true)
		val firstRandom = ScriptedBattleRandom(emptyList())
		val state = engine.start(
			initialState(
				first = participant("charge-user", speed = 100, skill = skill),
				second = participant("target", speed = 50),
			),
		)

		val afterFirst = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("charge-user", skillId = 1, targetActorId = "target")),
			firstRandom,
		)
		val afterSecond = engine.resolveTurn(afterFirst, emptyList(), ScriptedBattleRandom(listOf(1, 15)))

		scenario.assertNamed("charge-skill-releases-next-turn-without-extra-pp")
		val chargeStarted = afterSecond.events.filterIsInstance<BattleEvent.SkillChargeStarted>().single()
		assertEquals("charge-user", chargeStarted.actorId)
		assertEquals(1, chargeStarted.turnsRemainingBeforeUse)
		assertEquals(emptyList(), firstRandom.consumedReasons())
		assertEquals(34, afterFirst.participant("charge-user")?.skillSlot(1)?.remainingPp)
		assertEquals(1, afterFirst.participant("charge-user")?.chargingTurnsRemaining)
		assertEquals(100, afterFirst.participant("target")?.currentHp)
		assertEquals(emptyList(), afterFirst.events.filterIsInstance<BattleEvent.DamageApplied>())

		val chargeReleased = afterSecond.events.filterIsInstance<BattleEvent.SkillChargeReleased>().single()
		assertEquals("charge-user", chargeReleased.actorId)
		assertEquals(0, afterSecond.participant("charge-user")?.chargingTurnsRemaining)
		assertEquals(34, afterSecond.participant("charge-user")?.skillSlot(1)?.remainingPp)
		assertEquals(72, afterSecond.participant("target")?.currentHp)
		assertEquals(2, afterSecond.events.filterIsInstance<BattleEvent.SkillUsed>().size)
		assertEquals(1, afterSecond.events.filterIsInstance<BattleEvent.DamageApplied>().size)
	}

	@Test
	fun `charging participant cannot switch and still releases charged skill`() {
		val skill = damagingSkill(name = "蓄力替换测试", chargesBeforeUse = true)
		val state = engine.start(
			initialState(
				first = participant("charge-user", speed = 100, skill = skill),
				firstBench = listOf(participant("reserve", speed = 80)),
				second = participant("target", speed = 50),
			),
		)
		val afterFirst = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("charge-user", skillId = 1, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		val resolved = engine.resolveTurn(
			afterFirst,
			listOf(BattleAction.SwitchParticipant("charge-user", targetActorId = "reserve")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		val prevented = resolved.events.filterIsInstance<BattleEvent.SwitchPrevented>().filter { it.reason == SwitchPreventionReason.CHARGING }.single()
		assertEquals("charge-user", prevented.actorId)
		assertEquals(1, prevented.skillId)
		assertTrue(resolved.isActive("charge-user"))
		assertEquals(72, resolved.participant("target")?.currentHp)
		assertEquals(0, resolved.participant("charge-user")?.chargingTurnsRemaining)
	}

	@Test
	fun `configured weather skips charge and resolves skill immediately`() {
		val scenario = publicBattleRuleScenario(
			name = "charge-skill-skips-charge-in-sun",
			inputSummary = "使用者在晴天下使用被配置为跳过蓄力的特殊伤害技能。",
			expectedSummary = "技能只消耗一次 PP，不产生蓄力开始事件，并在同回合造成伤害。",
		)
		val skill = damagingSkill(
			name = "晴天蓄力测试",
			chargesBeforeUse = true,
			chargeSkippedByWeathers = setOf(BattleWeather.SUN),
		)
		val random = ScriptedBattleRandom(listOf(1, 15))
		val state = engine.start(
			initialState(
				first = participant("charge-user", speed = 100, skill = skill),
				second = participant("target", speed = 50),
				environment = BattleEnvironment(weather = BattleWeather.SUN),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("charge-user", skillId = 1, targetActorId = "target")),
			random,
		)

		scenario.assertNamed("charge-skill-skips-charge-in-sun")
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillChargeStarted>())
		assertEquals(0, resolved.participant("charge-user")?.chargingTurnsRemaining)
		assertEquals(34, resolved.participant("charge-user")?.skillSlot(1)?.remainingPp)
		assertEquals(72, resolved.participant("target")?.currentHp)
		assertEquals(1, resolved.events.filterIsInstance<BattleEvent.SkillUsed>().size)
		assertEquals(1, resolved.events.filterIsInstance<BattleEvent.DamageApplied>().size)
	}

	@Test
	fun `charge skipping item is consumed and resolves skill immediately`() {
		val scenario = publicBattleRuleScenario(
			name = "charge-skill-consumes-item-to-skip-charge",
			inputSummary = "使用者携带一次性跳过蓄力道具，使用需要蓄力的特殊伤害技能。",
			expectedSummary = "技能宣告后道具被消费，不产生蓄力开始事件，并在同回合造成伤害。",
		)
		val skill = damagingSkill(name = "道具蓄力测试", chargesBeforeUse = true)
		val state = engine.start(
			initialState(
				first = participant(
					"charge-user",
					speed = 100,
					skill = skill,
					itemId = 248,
					itemEffects = listOf(BattleItemEffect.ChargeSkipOnce()),
				),
				second = participant("target", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("charge-user", skillId = 1, targetActorId = "target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		scenario.assertNamed("charge-skill-consumes-item-to-skip-charge")
		val skipped = resolved.events.filterIsInstance<BattleEvent.SkillChargeSkippedByItem>().single()
		assertEquals("charge-user", skipped.actorId)
		assertEquals(1, skipped.skillId)
		assertEquals(248, skipped.itemId)
		assertTrue(skipped.consumed)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillChargeStarted>())
		assertEquals(0, resolved.participant("charge-user")?.chargingTurnsRemaining)
		assertEquals(null, resolved.participant("charge-user")?.itemId)
		assertEquals(emptyList(), resolved.participant("charge-user")?.itemEffects)
		assertEquals(34, resolved.participant("charge-user")?.skillSlot(1)?.remainingPp)
		assertEquals(72, resolved.participant("target")?.currentHp)
	}
}
