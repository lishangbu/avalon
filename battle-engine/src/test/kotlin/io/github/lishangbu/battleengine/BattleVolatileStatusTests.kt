package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.SkillPreventionReason
import io.github.lishangbu.battleengine.model.BattleStatusBlockReason
import io.github.lishangbu.battleengine.model.BattleVolatileStatus
import io.github.lishangbu.battleengine.model.BattleVolatileStatusApplication
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证畏缩和混乱等临时状态。
 *
 * 场景类型：行动前钩子级公开规则 场景。
 * 参考来源类型：公开成熟模拟器实现和公开规则说明。畏缩只阻止尚未行动成员的本回合行动；
 * 混乱使用 2..5 的内部计数，行动前递减，未解除时按 33% 概率造成 40 威力物理自伤。
 * 验证重点：临时状态不消耗被阻止行动的 PP，回合末畏缩不跨回合，混乱自伤会消费独立伤害随机数。
 */
class BattleVolatileStatusTests {
	private val engine = BattleEngine()

	@Test
	fun `flinch prevents slower target action without pp loss`() {
		val scenario = publicBattleRuleScenario(
			name = "flinch-prevents-slower-target-before-move",
			inputSummary = "较快成员先造成伤害并附加畏缩，较慢目标本回合也选择使用普通攻击。",
			expectedSummary = "目标本回合无法行动，不消耗 PP；畏缩在阻止行动后立即清除。",
		)
		val flinchSkill = damagingSkill(
			name = "畏缩测试",
			volatileStatusApplications = listOf(
				BattleVolatileStatusApplication(
					status = BattleVolatileStatus.FLINCH,
					target = BattleEffectTarget.TARGET,
					chancePercent = 100,
				),
			),
		)
		val state = engine.start(
			initialState(
				first = participant("fast", speed = 100, skill = flinchSkill),
				second = participant("slow", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("fast", skillId = 1, targetActorId = "slow"),
				BattleAction.UseSkill("slow", skillId = 1, targetActorId = "fast"),
			),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		scenario.assertNamed("flinch-prevents-slower-target-before-move")
		assertEquals(72, resolved.participant("slow")?.currentHp)
		assertEquals(100, resolved.participant("fast")?.currentHp)
		assertEquals(35, resolved.participant("slow")?.skillSlot(1)?.remainingPp)
		assertEquals(false, resolved.participant("slow")?.flinched)
		assertEquals("slow", resolved.events.filterIsInstance<BattleEvent.VolatileStatusApplied>().single().targetActorId)
		val blocked = resolved.events.filterIsInstance<BattleEvent.SkillPrevented>().filter { it.reason == SkillPreventionReason.VOLATILE_STATUS }.single()
		assertEquals("slow", blocked.actorId)
		assertEquals(BattleVolatileStatus.FLINCH, blocked.status)
	}

	@Test
	fun `flinch applied after target moved is cleared before next turn`() {
		val scenario = publicBattleRuleScenario(
			name = "late-flinch-does-not-carry-to-next-turn",
			inputSummary = "较快目标已经行动后，较慢成员命中并附加畏缩；下一回合较快目标再次行动。",
			expectedSummary = "第一回合晚到的畏缩不会阻止已经行动的目标，也不会保留到下一回合。",
		)
		val flinchSkill = damagingSkill(
			name = "慢速畏缩测试",
			volatileStatusApplications = listOf(
				BattleVolatileStatusApplication(
					status = BattleVolatileStatus.FLINCH,
					target = BattleEffectTarget.TARGET,
					chancePercent = 100,
				),
			),
		)
		val state = engine.start(
			initialState(
				first = participant("slow-flinch-user", speed = 50, skill = flinchSkill),
				second = participant("fast-target", speed = 100),
			),
		)

		val afterFirst = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("slow-flinch-user", skillId = 1, targetActorId = "fast-target"),
				BattleAction.UseSkill("fast-target", skillId = 1, targetActorId = "slow-flinch-user"),
			),
			ScriptedBattleRandom(listOf(1, 15, 1, 15)),
		)
		val afterSecond = engine.resolveTurn(
			afterFirst,
			listOf(BattleAction.UseSkill("fast-target", skillId = 1, targetActorId = "slow-flinch-user")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		scenario.assertNamed("late-flinch-does-not-carry-to-next-turn")
		assertEquals(false, afterFirst.participant("fast-target")?.flinched)
		assertEquals(emptyList(), afterFirst.events.filterIsInstance<BattleEvent.SkillPrevented>().filter { it.reason == SkillPreventionReason.VOLATILE_STATUS })
		assertEquals(44, afterSecond.participant("slow-flinch-user")?.currentHp)
		assertEquals(33, afterSecond.participant("fast-target")?.skillSlot(1)?.remainingPp)
	}

	@Test
	fun `confusion can self damage then clear before a later action`() {
		val scenario = publicBattleRuleScenario(
			name = "confusion-self-damage-and-later-clear",
			inputSummary = "较快成员用变化技能让目标混乱，固定内部计数为 3；目标随后三次尝试行动。",
			expectedSummary = "第一次行动前自伤并跳过技能，第二次通过混乱判定并正常攻击，第三次先解除混乱再正常攻击。",
		)
		val confusionSkill = damagingSkill(
			damageClass = BattleDamageClass.STATUS,
			volatileStatusApplications = listOf(
				BattleVolatileStatusApplication(
					status = BattleVolatileStatus.CONFUSION,
					target = BattleEffectTarget.TARGET,
					chancePercent = 100,
				),
			),
		)
		val state = engine.start(
			initialState(
				first = participant("confusion-user", speed = 100, skill = confusionSkill),
				second = participant("target", speed = 50),
			),
		)

		val afterFirst = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("confusion-user", skillId = 1, targetActorId = "target"),
				BattleAction.UseSkill("target", skillId = 1, targetActorId = "confusion-user"),
			),
			ScriptedBattleRandom(listOf(1, 0, 15)),
		)
		val afterSecond = engine.resolveTurn(
			afterFirst,
			listOf(BattleAction.UseSkill("target", skillId = 1, targetActorId = "confusion-user")),
			ScriptedBattleRandom(listOf(99, 1, 15)),
		)
		val afterThird = engine.resolveTurn(
			afterSecond,
			listOf(BattleAction.UseSkill("target", skillId = 1, targetActorId = "confusion-user")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		scenario.assertNamed("confusion-self-damage-and-later-clear")
		assertEquals(2, afterFirst.participant("target")?.confusionTurnsRemaining)
		assertEquals(81, afterFirst.participant("target")?.currentHp)
		assertEquals(35, afterFirst.participant("target")?.skillSlot(1)?.remainingPp)
		val selfDamage = afterFirst.events.filterIsInstance<BattleEvent.ConfusionDamageApplied>().single()
		assertEquals(19, selfDamage.amount)
		assertEquals(100, selfDamage.randomPercent)
		assertEquals(3, selfDamage.turnsRemainingBefore)

		assertEquals(1, afterSecond.participant("target")?.confusionTurnsRemaining)
		assertEquals(72, afterSecond.participant("confusion-user")?.currentHp)
		assertEquals(34, afterSecond.participant("target")?.skillSlot(1)?.remainingPp)

		assertEquals(0, afterThird.participant("target")?.confusionTurnsRemaining)
		assertEquals(44, afterThird.participant("confusion-user")?.currentHp)
		assertEquals(33, afterThird.participant("target")?.skillSlot(1)?.remainingPp)
		val cleared = afterThird.events.filterIsInstance<BattleEvent.VolatileStatusCleared>().single()
		assertEquals("target", cleared.actorId)
		assertEquals(BattleVolatileStatus.CONFUSION, cleared.status)
	}

	@Test
	fun `existing confusion blocks new confusion without refreshing duration`() {
		val scenario = publicBattleRuleScenario(
			name = "existing-confusion-blocks-new-confusion-without-refresh",
			inputSummary = "目标已经处于剩余 3 次行动检查的混乱状态，随后再次被 100% 附加混乱的变化技能命中。",
			expectedSummary = "目标仍保留原有混乱持续计数，不刷新到新的随机持续时间；事件流记录 EXISTING_STATUS，且不消费混乱持续随机数。",
		)
		val confusionSkill = damagingSkill(
			damageClass = BattleDamageClass.STATUS,
			volatileStatusApplications = listOf(
				BattleVolatileStatusApplication(
					status = BattleVolatileStatus.CONFUSION,
					target = BattleEffectTarget.TARGET,
					chancePercent = 100,
				),
			),
		)
		val random = ScriptedBattleRandom(emptyList())
		val state = engine.start(
			initialState(
				first = participant("confusion-user", speed = 100, skill = confusionSkill),
				second = participant("already-confused-target", speed = 50)
					.applyVolatileStatus(BattleVolatileStatus.CONFUSION, confusionTurnsRemaining = 3),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("confusion-user", skillId = 1, targetActorId = "already-confused-target")),
			random,
		)

		scenario.assertNamed("existing-confusion-blocks-new-confusion-without-refresh")
		assertEquals(emptyList(), random.consumedReasons())
		assertEquals(3, resolved.participant("already-confused-target")?.confusionTurnsRemaining)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.VolatileStatusApplied>())
		val blocked = resolved.events.filterIsInstance<BattleEvent.VolatileStatusApplicationBlocked>().single()
		assertEquals("already-confused-target", blocked.targetActorId)
		assertEquals(BattleVolatileStatus.CONFUSION, blocked.status)
		assertEquals(BattleStatusBlockReason.EXISTING_STATUS, blocked.reason)
	}
}
