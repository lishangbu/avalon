package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleVolatileStatus
import io.github.lishangbu.battleengine.model.BattleVolatileStatusApplication
import io.github.lishangbu.battleengine.model.SkillPreventionReason
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 验证依赖目标本回合待执行行动的条件攻击。
 *
 * 场景类型：技能使用前 gate 级公开规则场景。
 * 参考来源类型：公开成熟模拟器实现。突袭读取目标是否仍在队列中准备伤害技能；快手还击读取目标是否仍在队列中
 * 准备先制度伤害技能，并在命中后 100% 附加畏缩。两类技能都不是根据目标最终是否真的行动来回溯结算，而是在
 * 使用者宣告技能、消耗 PP 之后立即读取当前行动计划。
 * 验证重点：目标切换、已经行动、准备变化技能或准备普通优先度攻击时，条件技能在命中前失败；条件满足时才进入
 * 普通命中、伤害和附加效果。
 */
class BattlePendingTargetActionSkillTests {
	private val engine = BattleEngine()

	@Test
	fun `pending damaging target skill allows sucker punch style attack`() {
		val scenario = publicBattleRuleScenario(
			name = "pending-damaging-target-skill-allows-sucker-punch-style-attack",
			inputSummary = "使用者以先制度条件攻击命中仍未行动且准备伤害技能的目标。",
			expectedSummary = "条件攻击成功造成伤害；目标若仍可行动，随后照常执行自己的伤害技能。",
		)
		val state = engine.start(
			initialState(
				first = participant("ambusher", speed = 80, skill = suckerPunchSkill()),
				second = participant("attacker", speed = 100, skill = damagingSkill()),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("ambusher", skillId = 389, targetActorId = "attacker"),
				BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "ambusher"),
			),
			ScriptedBattleRandom(listOf(1, 15, 1, 15)),
		)

		scenario.assertNamed("pending-damaging-target-skill-allows-sucker-punch-style-attack")
		assertTrue(requireNotNull(resolved.participant("attacker")).currentHp < 100)
		assertTrue(requireNotNull(resolved.participant("ambusher")).currentHp < 100)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillFailed>())
		assertEquals(34, resolved.participant("ambusher")?.skillSlot(389)?.remainingPp)
	}

	@Test
	fun `pending status target skill fails sucker punch style attack before damage`() {
		val scenario = publicBattleRuleScenario(
			name = "pending-status-target-skill-fails-sucker-punch-style-attack",
			inputSummary = "目标本回合仍未行动，但准备的是变化技能而不是伤害技能。",
			expectedSummary = "条件攻击已经宣告并消耗 PP，但不会进入命中和伤害流程。",
		)
		val state = engine.start(
			initialState(
				first = participant("ambusher", speed = 80, skill = suckerPunchSkill()),
				second = participant("support", speed = 100, skill = emptyStatusSkill()),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("ambusher", skillId = 389, targetActorId = "support"),
				BattleAction.UseSkill("support", skillId = 2, targetActorId = "support"),
			),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("pending-status-target-skill-fails-sucker-punch-style-attack")
		assertEquals(100, resolved.participant("support")?.currentHp)
		assertEquals(100, resolved.participant("ambusher")?.currentHp)
		assertEquals(34, resolved.participant("ambusher")?.skillSlot(389)?.remainingPp)
		assertEquals(
			"target-not-pending-damaging-skill",
			resolved.events.filterIsInstance<BattleEvent.SkillFailed>().single().reason,
		)
	}

	@Test
	fun `already resolved target action fails pending target attack`() {
		val scenario = publicBattleRuleScenario(
			name = "already-resolved-target-action-fails-pending-target-attack",
			inputSummary = "目标使用更高优先度伤害技能先行动，随后使用者才尝试依赖目标待行动的攻击。",
			expectedSummary = "目标已经不再处于待行动队列，条件攻击宣告并消耗 PP 后失败。",
		)
		val fasterPriorityDamage = damagingSkill(skillId = 3, name = "先制度测试", priority = 2)
		val state = engine.start(
			initialState(
				first = participant("ambusher", speed = 80, skill = suckerPunchSkill()),
				second = participant("faster-priority", speed = 100, skill = fasterPriorityDamage),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("ambusher", skillId = 389, targetActorId = "faster-priority"),
				BattleAction.UseSkill("faster-priority", skillId = 3, targetActorId = "ambusher"),
			),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		scenario.assertNamed("already-resolved-target-action-fails-pending-target-attack")
		assertEquals(100, resolved.participant("faster-priority")?.currentHp)
		assertEquals(72, resolved.participant("ambusher")?.currentHp)
		assertEquals(
			"target-has-no-pending-skill-action",
			resolved.events.filterIsInstance<BattleEvent.SkillFailed>().single().reason,
		)
	}

	@Test
	fun `pending priority damaging target skill allows upper hand style attack and flinch`() {
		val scenario = publicBattleRuleScenario(
			name = "pending-priority-damaging-target-skill-allows-upper-hand-style-attack",
			inputSummary = "目标准备先制度伤害技能，使用者用更高优先度条件攻击拦截。",
			expectedSummary = "快手还击式技能成功造成伤害并附加畏缩，目标随后被畏缩阻止行动且不消耗 PP。",
		)
		val priorityDamage = damagingSkill(skillId = 4, name = "电光一闪测试", priority = 1)
		val state = engine.start(
			initialState(
				first = participant("counter", speed = 80, skill = upperHandSkill()),
				second = participant("priority-attacker", speed = 100, skill = priorityDamage),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("counter", skillId = 918, targetActorId = "priority-attacker"),
				BattleAction.UseSkill("priority-attacker", skillId = 4, targetActorId = "counter"),
			),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		scenario.assertNamed("pending-priority-damaging-target-skill-allows-upper-hand-style-attack")
		assertTrue(requireNotNull(resolved.participant("priority-attacker")).currentHp < 100)
		assertEquals(100, resolved.participant("counter")?.currentHp)
		assertEquals(14, resolved.participant("counter")?.skillSlot(918)?.remainingPp)
		assertEquals(35, resolved.participant("priority-attacker")?.skillSlot(4)?.remainingPp)
		assertEquals("priority-attacker", resolved.events.filterIsInstance<BattleEvent.VolatileStatusApplied>().single().targetActorId)
		val prevented = resolved.events.filterIsInstance<BattleEvent.SkillPrevented>().single()
		assertEquals("priority-attacker", prevented.actorId)
		assertEquals(SkillPreventionReason.VOLATILE_STATUS, prevented.reason)
	}

	@Test
	fun `ordinary priority target skill fails upper hand style attack`() {
		val scenario = publicBattleRuleScenario(
			name = "ordinary-priority-target-skill-fails-upper-hand-style-attack",
			inputSummary = "目标准备普通优先度伤害技能，使用者尝试快手还击式条件攻击。",
			expectedSummary = "条件攻击在命中前失败；目标随后按普通流程造成伤害。",
		)
		val state = engine.start(
			initialState(
				first = participant("counter", speed = 80, skill = upperHandSkill()),
				second = participant("ordinary-attacker", speed = 100, skill = damagingSkill()),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("counter", skillId = 918, targetActorId = "ordinary-attacker"),
				BattleAction.UseSkill("ordinary-attacker", skillId = 1, targetActorId = "counter"),
			),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		scenario.assertNamed("ordinary-priority-target-skill-fails-upper-hand-style-attack")
		assertEquals(100, resolved.participant("ordinary-attacker")?.currentHp)
		assertEquals(72, resolved.participant("counter")?.currentHp)
		assertEquals(14, resolved.participant("counter")?.skillSlot(918)?.remainingPp)
		assertEquals(
			"target-not-pending-priority-damaging-skill",
			resolved.events.filterIsInstance<BattleEvent.SkillFailed>().single().reason,
		)
	}

	private fun suckerPunchSkill() =
		damagingSkill(
			skillId = 389,
			name = "突袭",
			elementId = 17,
			power = 70,
			makesContact = true,
			requiresTargetPendingDamagingSkill = true,
			priority = 1,
		).copy(remainingPp = 35, maxPp = 35)

	private fun upperHandSkill() =
		damagingSkill(
			skillId = 918,
			name = "快手还击",
			elementId = 2,
			power = 65,
			makesContact = true,
			requiresTargetPendingPriorityDamagingSkill = true,
			priority = 3,
			volatileStatusApplications = listOf(
				BattleVolatileStatusApplication(
					status = BattleVolatileStatus.FLINCH,
					target = BattleEffectTarget.TARGET,
					chancePercent = 100,
				),
			),
		).copy(remainingPp = 15, maxPp = 15)

	private fun emptyStatusSkill() =
		damagingSkill(
			skillId = 2,
			name = "变化测试",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			affectedByProtect = false,
		)
}
