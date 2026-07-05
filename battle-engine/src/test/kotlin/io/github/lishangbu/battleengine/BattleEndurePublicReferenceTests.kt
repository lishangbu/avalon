package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleFatalDamageSurvivalSource
import io.github.lishangbu.battleengine.model.BattleFixedDamage
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 验证挺住在现代主系列规则下的保护类同族行为。
 *
 * 场景类型：挺住成功率、致命技能伤害保留 1 HP、同回合多次攻击继续保留、连续使用失败 场景。
 * 参考来源类型：公开技能资料和成熟对战实现均说明挺住与守住/看穿共享连续保护递减成功率；成功后不会阻挡技能命中，
 * 而是在本回合使用者遭受会导致倒下的攻击时至少保留 1 HP。它不保护回合末异常、天气等间接伤害，这里通过只接入
 * 技能伤害写入入口来固定边界，后续间接伤害测试不应观察到挺住事件。
 */
class BattleEndurePublicReferenceTests {
	private val engine = BattleEngine()

	@Test
	fun `endure keeps user at one hp without blocking the incoming skill`() {
		val scenario = publicBattleRuleScenario(
			name = "endure-keeps-user-at-one-hp-without-blocking-incoming-skill",
			inputSummary = "使用者当回合先成功使用挺住，随后受到足以倒下的固定技能伤害。",
			expectedSummary = "攻击没有被保护阻挡，伤害事件仍然产生；实际扣血被夹到剩余 1 HP，并记录技能来源的濒死保留事件。",
		)
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("endurer", speed = 100, currentHp = 75, skill = endureSkill()),
					second = participant("attacker", speed = 50, skill = fixedDamageSkill(amount = 200)),
				),
			),
			listOf(
				BattleAction.UseSkill("endurer", skillId = 203, targetActorId = "endurer"),
				BattleAction.UseSkill("attacker", skillId = 9002, targetActorId = "endurer"),
			),
			ScriptedBattleRandom(emptyList()),
		)
		val damage = resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single()
		val survived = resolved.events.filterIsInstance<BattleEvent.FatalDamageSurvived>().single()

		scenario.assertNamed("endure-keeps-user-at-one-hp-without-blocking-incoming-skill")
		assertEquals(1, resolved.participant("endurer")?.currentHp)
		assertNull(resolved.result)
		assertEquals(74, damage.amount)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillBlockedByProtection>())
		assertEquals("endurer", resolved.events.filterIsInstance<BattleEvent.FatalDamageEndureStarted>().single().actorId)
		assertEquals(BattleFatalDamageSurvivalSource.SKILL, survived.source)
		assertEquals(203, survived.sourceId)
		assertEquals(false, survived.consumed)
		assertEquals(200, survived.incomingDamage)
		assertEquals(126, survived.preventedDamage)
	}

	@Test
	fun `endure keeps protecting against multiple fatal attacks in the same turn`() {
		val scenario = publicBattleRuleScenario(
			name = "endure-keeps-protecting-against-multiple-fatal-attacks-in-same-turn",
			inputSummary = "双打中使用者先成功挺住，随后两个较慢对手在同一回合连续攻击该使用者。",
			expectedSummary = "挺住姿态不在第一次触发后消失；第一次攻击把使用者压到 1 HP，第二次致命攻击实际扣血为 0。",
		)
		val resolved = engine.resolveTurn(
			engine.start(
				doubleInitialState(
					firstA = participant("endurer", speed = 100, currentHp = 50, skill = endureSkill()),
					firstB = participant("ally", speed = 10),
					secondA = participant("first-attacker", speed = 50, skill = fixedDamageSkill(skillId = 9002, amount = 200)),
					secondB = participant("second-attacker", speed = 40, skill = fixedDamageSkill(skillId = 9003, amount = 200)),
				),
			),
			listOf(
				BattleAction.UseSkill("endurer", skillId = 203, targetActorId = "endurer"),
				BattleAction.UseSkill("first-attacker", skillId = 9002, targetActorId = "endurer"),
				BattleAction.UseSkill("second-attacker", skillId = 9003, targetActorId = "endurer"),
			),
			ScriptedBattleRandom(emptyList()),
		)
		val damageAmounts = resolved.events.filterIsInstance<BattleEvent.DamageApplied>().map { it.amount }
		val survivedEvents = resolved.events.filterIsInstance<BattleEvent.FatalDamageSurvived>()

		scenario.assertNamed("endure-keeps-protecting-against-multiple-fatal-attacks-in-same-turn")
		assertEquals(1, resolved.participant("endurer")?.currentHp)
		assertNull(resolved.result)
		assertEquals(listOf(49, 0), damageAmounts)
		assertEquals(2, survivedEvents.size)
		assertEquals(listOf(151, 200), survivedEvents.map { it.preventedDamage })
	}

	@Test
	fun `failed consecutive endure leaves user vulnerable to later damage`() {
		val scenario = publicBattleRuleScenario(
			name = "failed-consecutive-endure-leaves-user-vulnerable-to-later-damage",
			inputSummary = "使用者已有一次连续保护类成功计数，第二次使用挺住时掷点失败，较慢对手随后造成致命伤害。",
			expectedSummary = "挺住失败后不建立保留 HP 姿态，连续保护计数被清零，后续技能可以正常把使用者击倒。",
		)
		val endurer = participant("endurer", speed = 100, currentHp = 75, skill = endureSkill()).copy(protectionChain = 1)
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = endurer,
					second = participant("attacker", speed = 50, skill = fixedDamageSkill(amount = 200)),
				),
			),
			listOf(
				BattleAction.UseSkill("endurer", skillId = 203, targetActorId = "endurer"),
				BattleAction.UseSkill("attacker", skillId = 9002, targetActorId = "endurer"),
			),
			ScriptedBattleRandom(listOf(1)),
		)

		scenario.assertNamed("failed-consecutive-endure-leaves-user-vulnerable-to-later-damage")
		assertEquals(0, resolved.participant("endurer")?.currentHp)
		assertEquals(0, resolved.participant("endurer")?.protectionChain)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.FatalDamageEndureStarted>())
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.FatalDamageSurvived>())
		assertEquals(75, resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount)
	}

	private fun fixedDamageSkill(skillId: Long = 9002, amount: Int) =
		damagingSkill(
			skillId = skillId,
			name = "固定伤害测试",
			power = null,
			fixedDamage = BattleFixedDamage.FixedAmount(amount),
		)
}
