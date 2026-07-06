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
 * 验证命中后破除保护类屏障的伤害技能。
 *
 * 场景类型：保护 gate 与后续命中流程的公开规则场景。
 * 参考来源类型：公开成熟模拟器实现。公开引擎把佯攻标记为 `breaksProtect`：技能本身不会被保护拦下，命中后会移除
 * 目标个人保护屏障，以及目标侧的广域防守/快速防守等本回合临时侧防护。破除发生在命中判定通过之后、普通伤害
 * 写入之前，因此 replay 中应先看到破除事件，再看到伤害事件；同一回合更晚的技能也应读取到已经被移除的防护。
 * 验证重点：佯攻不绕过命中流程、不凭空写空破除事件、破除个人保护时会重置连续保护链，并且侧防护被破除后
 * 后续正优先度技能不再被快速防守拦截。
 */
class BattleProtectionBreakingSkillTests {
	private val engine = BattleEngine()

	@Test
	fun `feint breaks target protection and deals damage`() {
		val scenario = publicBattleRuleScenario(
			name = "feint-breaks-target-protection-and-deals-damage",
			inputSummary = "目标先使用保护类技能建立个人屏障，随后对手使用佯攻命中目标。",
			expectedSummary = "佯攻不被保护阻挡，命中后破除目标个人保护屏障并造成伤害，目标连续保护计数被清零。",
		)
		val state = engine.start(
			initialState(
				first = participant("protector", speed = 100, skill = protectionSkill()),
				second = participant("feint-user", speed = 50, skill = feintSkill()),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("protector", skillId = 2, targetActorId = "protector"),
				BattleAction.UseSkill("feint-user", skillId = 364, targetActorId = "protector"),
			),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		scenario.assertNamed("feint-breaks-target-protection-and-deals-damage")
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillBlockedByProtection>())
		val broken = resolved.events.filterIsInstance<BattleEvent.ProtectionBroken>().single()
		assertTrue(broken.brokeActorProtection)
		assertEquals(emptyList(), broken.brokenSideProtectionKinds)
		assertEquals("protector", broken.targetActorId)
		assertEquals("feint-user", resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().actorId)
		assertEquals(0, resolved.participant("protector")?.protectionChain)
		assertTrue(requireNotNull(resolved.participant("protector")).currentHp < 100)
	}

	@Test
	fun `feint breaks priority side guard before later priority attack`() {
		val scenario = publicBattleRuleScenario(
			name = "feint-breaks-priority-side-guard-before-later-priority-attack",
			inputSummary = "双打中目标侧先建立快速防守；对手随后用佯攻命中该侧成员，另一个对手再使用正优先度攻击。",
			expectedSummary = "佯攻移除目标侧本回合先制防护；后续正优先度攻击不再被快速防守阻挡并正常造成伤害。",
		)
		val priorityAttack = damagingSkill(skillId = 3, name = "先制测试", priority = 1)
		val state = engine.start(
			doubleInitialState(
				firstA = participant("guard-user", speed = 100, skill = prioritySideGuardSkill()),
				firstB = participant("guard-ally", speed = 90),
				secondA = participant("feint-user", speed = 80, skill = feintSkill()),
				secondB = participant("priority-attacker", speed = 70, skill = priorityAttack),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("guard-user", skillId = 501, targetActorId = "guard-user"),
				BattleAction.UseSkill("feint-user", skillId = 364, targetActorId = "guard-ally"),
				BattleAction.UseSkill("priority-attacker", skillId = 3, targetActorId = "guard-user"),
			),
			ScriptedBattleRandom(listOf(1, 15, 1, 15)),
		)

		scenario.assertNamed("feint-breaks-priority-side-guard-before-later-priority-attack")
		val broken = resolved.events.filterIsInstance<BattleEvent.ProtectionBroken>().single()
		assertEquals("side-a", broken.sideId)
		assertEquals(listOf(BattleSideProtectionKind.PRIORITY_SKILL), broken.brokenSideProtectionKinds)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillBlockedByProtection>())
		assertEquals("priority-attacker", resolved.events.filterIsInstance<BattleEvent.DamageApplied>().last().actorId)
		assertTrue(requireNotNull(resolved.participant("guard-user")).currentHp < 100)
	}

	@Test
	fun `feint without active protection does not create protection broken event`() {
		val scenario = publicBattleRuleScenario(
			name = "feint-without-active-protection-does-not-create-protection-broken-event",
			inputSummary = "目标没有任何个人保护或本回合侧防护时，对手直接使用佯攻。",
			expectedSummary = "佯攻按普通伤害技能结算，不写入空的保护破除事件。",
		)
		val state = engine.start(
			initialState(
				first = participant("target", speed = 100),
				second = participant("feint-user", speed = 50, skill = feintSkill()),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("feint-user", skillId = 364, targetActorId = "target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		scenario.assertNamed("feint-without-active-protection-does-not-create-protection-broken-event")
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.ProtectionBroken>())
		assertEquals("feint-user", resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().actorId)
	}

	private fun feintSkill(): BattleSkillSlot =
		damagingSkill(
			skillId = 364,
			name = "佯攻",
			damageClass = BattleDamageClass.PHYSICAL,
			power = 30,
			affectedByProtect = false,
			breaksProtection = true,
			priority = 2,
		).copy(remainingPp = 10, maxPp = 10)

	private fun prioritySideGuardSkill(): BattleSkillSlot =
		damagingSkill(
			skillId = 501,
			name = "快速防守",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			targetScope = BattleSkillTargetScope.SELF,
			affectedByProtect = false,
			protectsUserSideFromPrioritySkills = true,
			priority = 3,
		).copy(remainingPp = 15, maxPp = 15)
}
