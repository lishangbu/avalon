package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleSideProtectionKind
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleSkillTargetScope
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 验证本回合一侧临时防护技能。
 *
 * 场景类型：命中前保护 gate 级公开规则场景。
 * 参考来源类型：公开成熟模拟器实现。公开引擎中广域防守只阻挡范围目标技能，快速防守只阻挡有效优先度为正的技能；
 * 两者都只在本回合有效，都会进入连续保护递减，并且在行动队列里没有后续行动时不会建立防护。
 * 验证重点：防护建立发生在技能宣告和 PP 消耗之后、后续目标命中判定之前；阻挡事件复用保护阻挡语义，不消费命中
 * 或伤害随机数；普通单体技能、普通优先度技能和明确穿透保护的技能不应被误拦截。
 */
class BattleSideGuardSkillTests {
	private val engine = BattleEngine()

	@Test
	fun `multi target side guard blocks opponent spread skill before accuracy`() {
		val scenario = publicBattleRuleScenario(
			name = "multi-target-side-guard-blocks-opponent-spread-skill-before-accuracy",
			inputSummary = "双打中使用者先建立一侧范围防护，对手随后使用会影响两个己方目标的范围技能。",
			expectedSummary = "己方两个当前上场成员都被保护阻挡，不消费命中或伤害随机数，也不产生伤害事件。",
		)
		val spreadSkill = damagingSkill(targetScope = BattleSkillTargetScope.ALL_ADJACENT_OPPONENTS)
		val state = engine.start(
			doubleInitialState(
				firstA = participant("guard-user", speed = 100, skill = multiTargetSideGuardSkill()),
				firstB = participant("guard-ally", speed = 90),
				secondA = participant("spread-attacker", speed = 80, skill = spreadSkill),
				secondB = participant("observer", speed = 70),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("guard-user", skillId = 469, targetActorId = "guard-user"),
				BattleAction.UseSkill("spread-attacker", skillId = 1, targetActorId = "guard-user"),
			),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("multi-target-side-guard-blocks-opponent-spread-skill-before-accuracy")
		assertSideProtectionStarted(resolved.events, BattleSideProtectionKind.MULTI_TARGET_SKILL)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.DamageApplied>())
		assertEquals(
			listOf("guard-user", "guard-ally"),
			resolved.events.filterIsInstance<BattleEvent.SkillBlockedByProtection>().map { it.targetActorId },
		)
		assertEquals(9, resolved.participant("guard-user")?.skillSlot(469)?.remainingPp)
		assertEquals(1, resolved.participant("guard-user")?.protectionChain)
	}

	@Test
	fun `multi target side guard does not block ordinary single target skill`() {
		val scenario = publicBattleRuleScenario(
			name = "multi-target-side-guard-does-not-block-single-target-skill",
			inputSummary = "双打中使用者建立一侧范围防护，对手随后使用普通单体伤害技能。",
			expectedSummary = "范围防护不会阻挡单体技能；目标照常进入命中和伤害流程。",
		)
		val state = engine.start(
			doubleInitialState(
				firstA = participant("guard-user", speed = 100, skill = multiTargetSideGuardSkill()),
				firstB = participant("guard-ally", speed = 90),
				secondA = participant("single-attacker", speed = 80),
				secondB = participant("observer", speed = 70),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("guard-user", skillId = 469, targetActorId = "guard-user"),
				BattleAction.UseSkill("single-attacker", skillId = 1, targetActorId = "guard-user"),
			),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		scenario.assertNamed("multi-target-side-guard-does-not-block-single-target-skill")
		assertSideProtectionStarted(resolved.events, BattleSideProtectionKind.MULTI_TARGET_SKILL)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillBlockedByProtection>())
		assertEquals(72, resolved.participant("guard-user")?.currentHp)
	}

	@Test
	fun `priority side guard blocks opponent positive priority skill`() {
		val scenario = publicBattleRuleScenario(
			name = "priority-side-guard-blocks-opponent-positive-priority-skill",
			inputSummary = "双打中使用者先建立一侧先制防护，对手随后使用正优先度伤害技能。",
			expectedSummary = "先制防护在命中前阻挡目标，不消费命中或伤害随机数，也不产生伤害事件。",
		)
		val prioritySkill = damagingSkill(skillId = 3, name = "先制测试", priority = 1)
		val state = engine.start(
			doubleInitialState(
				firstA = participant("guard-user", speed = 100, skill = prioritySideGuardSkill()),
				firstB = participant("guard-ally", speed = 90),
				secondA = participant("priority-attacker", speed = 80, skill = prioritySkill),
				secondB = participant("observer", speed = 70),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("guard-user", skillId = 501, targetActorId = "guard-user"),
				BattleAction.UseSkill("priority-attacker", skillId = 3, targetActorId = "guard-user"),
			),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("priority-side-guard-blocks-opponent-positive-priority-skill")
		assertSideProtectionStarted(resolved.events, BattleSideProtectionKind.PRIORITY_SKILL)
		assertEquals("guard-user", resolved.events.filterIsInstance<BattleEvent.SkillBlockedByProtection>().single().targetActorId)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.DamageApplied>())
		assertEquals(14, resolved.participant("guard-user")?.skillSlot(501)?.remainingPp)
	}

	@Test
	fun `priority side guard ignores ordinary priority skill`() {
		val scenario = publicBattleRuleScenario(
			name = "priority-side-guard-ignores-ordinary-priority-skill",
			inputSummary = "双打中使用者建立一侧先制防护，对手随后使用普通优先度伤害技能。",
			expectedSummary = "先制防护不会阻挡普通优先度技能；目标照常受到伤害。",
		)
		val state = engine.start(
			doubleInitialState(
				firstA = participant("guard-user", speed = 100, skill = prioritySideGuardSkill()),
				firstB = participant("guard-ally", speed = 90),
				secondA = participant("ordinary-attacker", speed = 80),
				secondB = participant("observer", speed = 70),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("guard-user", skillId = 501, targetActorId = "guard-user"),
				BattleAction.UseSkill("ordinary-attacker", skillId = 1, targetActorId = "guard-user"),
			),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		scenario.assertNamed("priority-side-guard-ignores-ordinary-priority-skill")
		assertSideProtectionStarted(resolved.events, BattleSideProtectionKind.PRIORITY_SKILL)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillBlockedByProtection>())
		assertEquals(72, resolved.participant("guard-user")?.currentHp)
	}

	@Test
	fun `side guard fails after declaration when no later skill action exists`() {
		val scenario = publicBattleRuleScenario(
			name = "side-guard-fails-after-declaration-when-no-later-skill-action-exists",
			inputSummary = "单打中唯一行动者使用一侧临时防护，行动队列中没有任何后续技能行动。",
			expectedSummary = "技能已经宣告并消耗 PP，但不会建立一侧防护，也不会推进连续保护计数。",
		)
		val state = engine.start(
			initialState(
				first = participant("guard-user", speed = 100, skill = prioritySideGuardSkill()),
				second = participant("observer", speed = 80),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("guard-user", skillId = 501, targetActorId = "guard-user")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("side-guard-fails-after-declaration-when-no-later-skill-action-exists")
		assertEquals(14, resolved.participant("guard-user")?.skillSlot(501)?.remainingPp)
		assertEquals(0, resolved.participant("guard-user")?.protectionChain)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SideProtectionStarted>())
		assertEquals(
			"no-pending-skill-action-after-side-protection",
			resolved.events.filterIsInstance<BattleEvent.SkillFailed>().single().reason,
		)
	}

	@Test
	fun `consecutive side guard can fail by shared protection chain`() {
		val scenario = publicBattleRuleScenario(
			name = "consecutive-side-guard-can-fail-by-shared-protection-chain",
			inputSummary = "使用者上一回合已经成功建立一侧先制防护，本回合再次连续使用同族防护。",
			expectedSummary = "第二次使用进入连续保护递减；随机失败后不会建立侧防护，对手先制技能照常造成伤害。",
		)
		val ordinarySkill = damagingSkill()
		val prioritySkill = damagingSkill(skillId = 3, name = "先制测试", priority = 1)
		val state = engine.start(
			doubleInitialState(
				firstA = participant("guard-user", speed = 100, skill = prioritySideGuardSkill()),
				firstB = participant("guard-ally", speed = 90),
				secondA = participant("attacker", speed = 80, skill = ordinarySkill),
				secondB = participant("observer", speed = 70),
			),
		)
		val afterFirstGuard = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("guard-user", skillId = 501, targetActorId = "guard-user"),
				BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "guard-user"),
			),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val afterSkillSwap = afterFirstGuard.replaceParticipant(
			requireNotNull(afterFirstGuard.participant("attacker")) {
				"attacker should remain present"
			}.copy(skillSlots = listOf(prioritySkill)),
		)

		val resolved = engine.resolveTurn(
			afterSkillSwap,
			listOf(
				BattleAction.UseSkill("guard-user", skillId = 501, targetActorId = "guard-user"),
				BattleAction.UseSkill("attacker", skillId = 3, targetActorId = "guard-user"),
			),
			ScriptedBattleRandom(listOf(1, 1, 15)),
		)
		val secondTurnEvents = resolved.events.drop(afterSkillSwap.events.size)

		scenario.assertNamed("consecutive-side-guard-can-fail-by-shared-protection-chain")
		assertEquals(1, afterSkillSwap.participant("guard-user")?.protectionChain)
		assertEquals(0, resolved.participant("guard-user")?.protectionChain)
		assertEquals(44, resolved.participant("guard-user")?.currentHp)
		assertEquals("guard-user", secondTurnEvents.filterIsInstance<BattleEvent.ProtectionFailed>().single().actorId)
		assertEquals(emptyList(), secondTurnEvents.filterIsInstance<BattleEvent.SideProtectionStarted>())
	}

	private fun assertSideProtectionStarted(events: List<BattleEvent>, kind: BattleSideProtectionKind) {
		val protection = events.filterIsInstance<BattleEvent.SideProtectionStarted>().single()
		assertEquals(kind, protection.kind)
		assertEquals("side-a", protection.sideId)
		assertEquals(null, protection.turnsRemaining)
	}

	private fun multiTargetSideGuardSkill(): BattleSkillSlot =
		sideGuardSkill(
			skillId = 469,
			name = "广域防守",
			maxPp = 10,
			protectsUserSideFromMultiTargetSkills = true,
		)

	private fun prioritySideGuardSkill(): BattleSkillSlot =
		sideGuardSkill(
			skillId = 501,
			name = "快速防守",
			maxPp = 15,
			protectsUserSideFromPrioritySkills = true,
		)

	private fun sideGuardSkill(
		skillId: Long,
		name: String,
		maxPp: Int,
		protectsUserSideFromMultiTargetSkills: Boolean = false,
		protectsUserSideFromPrioritySkills: Boolean = false,
	): BattleSkillSlot =
		damagingSkill(
			skillId = skillId,
			name = name,
			damageClass = BattleDamageClass.STATUS,
			power = null,
			targetScope = BattleSkillTargetScope.SELF,
			affectedByProtect = false,
			protectsUserSideFromMultiTargetSkills = protectsUserSideFromMultiTargetSkills,
			protectsUserSideFromPrioritySkills = protectsUserSideFromPrioritySkills,
			priority = 3,
		).copy(remainingPp = maxPp, maxPp = maxPp)
}
