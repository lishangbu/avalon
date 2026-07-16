package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.SwitchPreventionReason
import io.github.lishangbu.battleengine.model.BattleFixedDamage
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleVolatileStatus
import io.github.lishangbu.battleengine.model.BattleVolatileStatusApplication
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 验证束缚类临时状态的现代主系列规则。
 *
 * 场景类型：技能临时状态、主动替换限制与回合末间接伤害 场景。
 * 参考来源类型：公开成熟模拟器中的技能资料和状态条件说明，以及中文公开规则资料。现代规则中，束缚类技能命中后
 * 会让目标在 4..5 回合内不能主动替换，并在每个回合末承受最大 HP 的 1/8 间接伤害；束缚来源离场或无法战斗时，
 * 目标身上的束缚立即结束。
 * 验证重点：束缚写入来源和持续回合；被束缚成员主动替换失败；回合末伤害和持续回合递减；持续归零时解除；
 * 来源离场会清除目标束缚且不再造成回合末伤害。
 */
class BattleBindingStatusTests {
	private val engine = BattleEngine()

	@Test
	fun `switch restriction immunity item lets bound participant switch`() {
		val state = engine.start(
			initialState(
				first = participant(
					"bound",
					speed = 100,
					itemId = 295,
					itemEffects = listOf(BattleItemEffect.SwitchRestrictionImmunity()),
				).copy(boundByActorId = "binder", bindingTurnsRemaining = 3),
				firstBench = listOf(participant("reserve", speed = 80)),
				second = participant("binder", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.SwitchParticipant("bound", targetActorId = "reserve")),
			ScriptedBattleRandom(emptyList()),
		)

		assertTrue(resolved.isActive("reserve"))
		assertTrue(resolved.events.filterIsInstance<BattleEvent.SwitchPrevented>().isEmpty())
	}

	@Test
	fun `binding skill traps target and deals end turn damage`() {
		val scenario = publicBattleRuleScenario(
			name = "binding-skill-traps-target-and-deals-end-turn-damage",
			inputSummary = "使用者用束缚类技能命中目标，持续回合随机结果为 4。",
			expectedSummary = "目标记录束缚来源并在回合末承受最大 HP 的 1/8 伤害，剩余回合递减为 3。",
		)
		val random = ScriptedBattleRandom(listOf(0))
		val state = engine.start(
			initialState(
				first = participant("binder", speed = 100, skill = bindingSkill()),
				second = participant("target", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("binder", skillId = 20, targetActorId = "target")),
			random,
		)

		scenario.assertNamed("binding-skill-traps-target-and-deals-end-turn-damage")
		assertEquals(listOf("binding duration for 20"), random.consumedReasons())
		val applied = resolved.events.filterIsInstance<BattleEvent.VolatileStatusApplied>().single()
		assertEquals(BattleVolatileStatus.BINDING, applied.status)
		assertEquals("binder", resolved.participant("target")?.boundByActorId)
		assertEquals(3, resolved.participant("target")?.bindingTurnsRemaining)
		assertEquals(68, resolved.participant("target")?.currentHp)
		val bindingDamage = resolved.events.filterIsInstance<BattleEvent.BindingDamageApplied>().single()
		assertEquals(12, bindingDamage.amount)
		assertEquals(4, bindingDamage.turnsRemainingBefore)
	}

	@Test
	fun `bound participant cannot switch voluntarily`() {
		val scenario = publicBattleRuleScenario(
			name = "bound-participant-cannot-switch-voluntarily",
			inputSummary = "成员处于束缚状态且束缚来源仍在场，尝试主动替换。",
			expectedSummary = "主动替换被阻止，成员仍留在上场席位，并在回合末继续承受束缚伤害。",
		)
		val state = engine.start(
			initialState(
				first = participant("bound", speed = 100).copy(boundByActorId = "binder", bindingTurnsRemaining = 3),
				firstBench = listOf(participant("reserve", speed = 80)),
				second = participant("binder", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.SwitchParticipant("bound", targetActorId = "reserve")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("bound-participant-cannot-switch-voluntarily")
		val prevented = resolved.events.filterIsInstance<BattleEvent.SwitchPrevented>().filter { it.reason == SwitchPreventionReason.BINDING }.single()
		assertEquals("bound", prevented.actorId)
		assertEquals("binder", prevented.sourceActorId)
		assertEquals(3, prevented.turnsRemainingBefore)
		assertTrue(resolved.isActive("bound"))
		assertEquals(88, resolved.participant("bound")?.currentHp)
		assertEquals(2, resolved.participant("bound")?.bindingTurnsRemaining)
	}

	@Test
	fun `bound participant takes end turn binding damage`() {
		val scenario = publicBattleRuleScenario(
			name = "bound-participant-takes-end-turn-binding-damage",
			inputSummary = "目标处于束缚状态，来源仍在场，双方本回合没有行动。",
			expectedSummary = "目标在回合末受到最大 HP 的 1/8 伤害，束缚剩余回合减少 1。",
		)
		val state = engine.start(
			initialState(
				first = participant("binder", speed = 100),
				second = participant("target", speed = 50).copy(boundByActorId = "binder", bindingTurnsRemaining = 3),
			),
		)

		val resolved = engine.resolveTurn(state, emptyList(), ScriptedBattleRandom(emptyList()))

		scenario.assertNamed("bound-participant-takes-end-turn-binding-damage")
		val bindingDamage = resolved.events.filterIsInstance<BattleEvent.BindingDamageApplied>().single()
		assertEquals("target", bindingDamage.actorId)
		assertEquals("binder", bindingDamage.sourceActorId)
		assertEquals(12, bindingDamage.amount)
		assertEquals(88, resolved.participant("target")?.currentHp)
		assertEquals(2, resolved.participant("target")?.bindingTurnsRemaining)
	}

	@Test
	fun `binding clears when end turn duration reaches zero`() {
		val scenario = publicBattleRuleScenario(
			name = "binding-clears-when-end-turn-duration-reaches-zero",
			inputSummary = "目标束缚只剩 1 回合，来源仍在场，双方本回合没有行动。",
			expectedSummary = "目标在回合末承受最后一次束缚伤害，随后束缚归零并产生临时状态解除事件。",
		)
		val state = engine.start(
			initialState(
				first = participant("binder", speed = 100),
				second = participant("target", speed = 50).copy(boundByActorId = "binder", bindingTurnsRemaining = 1),
			),
		)

		val resolved = engine.resolveTurn(state, emptyList(), ScriptedBattleRandom(emptyList()))

		scenario.assertNamed("binding-clears-when-end-turn-duration-reaches-zero")
		assertEquals(null, resolved.participant("target")?.boundByActorId)
		assertEquals(0, resolved.participant("target")?.bindingTurnsRemaining)
		val cleared = resolved.events.filterIsInstance<BattleEvent.VolatileStatusCleared>().single()
		assertEquals("target", cleared.actorId)
		assertEquals(BattleVolatileStatus.BINDING, cleared.status)
		assertEquals(88, resolved.participant("target")?.currentHp)
	}

	@Test
	fun `binding clears when source switches out`() {
		val scenario = publicBattleRuleScenario(
			name = "binding-clears-when-source-switches-out",
			inputSummary = "束缚来源主动替换离场，目标仍处于束缚状态。",
			expectedSummary = "目标束缚立即解除，本回合末不会再受到该束缚的间接伤害。",
		)
		val state = engine.start(
			initialState(
				first = participant("binder", speed = 100),
				firstBench = listOf(participant("reserve", speed = 80)),
				second = participant("target", speed = 50).copy(boundByActorId = "binder", bindingTurnsRemaining = 3),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.SwitchParticipant("binder", targetActorId = "reserve")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("binding-clears-when-source-switches-out")
		assertTrue(resolved.isActive("reserve"))
		assertEquals(null, resolved.participant("target")?.boundByActorId)
		assertEquals(0, resolved.participant("target")?.bindingTurnsRemaining)
		assertEquals(100, resolved.participant("target")?.currentHp)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.BindingDamageApplied>())
		val cleared = resolved.events.filterIsInstance<BattleEvent.VolatileStatusCleared>().single()
		assertEquals(BattleVolatileStatus.BINDING, cleared.status)
		assertEquals("target", cleared.actorId)
	}

	private fun bindingSkill() =
		damagingSkill(
			skillId = 20,
			name = "绑紧",
			damageClass = BattleDamageClass.PHYSICAL,
			power = null,
			fixedDamage = BattleFixedDamage.FixedAmount(20),
			volatileStatusApplications = listOf(
				BattleVolatileStatusApplication(
					status = BattleVolatileStatus.BINDING,
					target = BattleEffectTarget.TARGET,
					chancePercent = 100,
				),
			),
		)
}
