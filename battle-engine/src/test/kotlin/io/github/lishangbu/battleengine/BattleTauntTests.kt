package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.SkillPreventionReason
import io.github.lishangbu.battleengine.model.BattleFixedDamage
import io.github.lishangbu.battleengine.model.BattleVolatileStatus
import io.github.lishangbu.battleengine.model.BattleVolatileStatusApplication
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 验证挑衅这一临时状态的现代主系列规则。
 *
 * 场景类型：技能临时状态与行动选择限制 场景。
 * 参考来源类型：公开成熟模拟器中的技能资料和状态条件说明，以及中文公开规则资料。现代规则中，处于挑衅的
 * 成员不能使用变化分类技能，但仍可以使用物理或特殊分类技能；挑衅按固定持续回合在回合末递减，已有挑衅不会
 * 因重复附加而刷新持续时间。
 * 验证重点：挑衅由变化技能命中后写入目标；被挑衅成员的变化技能在 PP 消耗前失败；攻击分类技能继续结算；
 * 状态能自然解除，并且已有挑衅不刷新旧计数。
 */
class BattleTauntTests {
	private val engine = BattleEngine()

	@Test
	fun `status skill applies taunt to target for three turns`() {
		val scenario = publicBattleRuleScenario(
			name = "status-skill-applies-taunt-to-target",
			inputSummary = "使用者用挑衅类变化技能命中目标。",
			expectedSummary = "目标获得挑衅临时状态；当前回合结束后剩余 2 回合。",
		)
		val state = engine.start(
			initialState(
				first = participant("taunt-user", speed = 100, skill = tauntSkill()),
				second = participant("target", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("taunt-user", skillId = 269, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("status-skill-applies-taunt-to-target")
		val applied = resolved.events.filterIsInstance<BattleEvent.VolatileStatusApplied>().single()
		assertEquals(BattleVolatileStatus.TAUNT, applied.status)
		assertEquals("target", applied.targetActorId)
		assertEquals(2, resolved.participant("target")?.tauntTurnsRemaining)
	}

	@Test
	fun `taunted participant cannot use status skill`() {
		val scenario = publicBattleRuleScenario(
			name = "taunted-participant-cannot-use-status-skill",
			inputSummary = "处于挑衅的成员尝试使用变化分类技能。",
			expectedSummary = "变化技能在 PP 消耗前失败，成员不会产生技能使用事件。",
		)
		val state = engine.start(
			initialState(
				first = participant("taunted", speed = 100, skill = statusSkill())
					.copy(tauntTurnsRemaining = 2),
				second = participant("observer", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("taunted", skillId = 14, targetActorId = "taunted")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("taunted-participant-cannot-use-status-skill")
		val blocked = resolved.events.filterIsInstance<BattleEvent.SkillPrevented>().filter { it.reason == SkillPreventionReason.TAUNT }.single()
		assertEquals("taunted", blocked.actorId)
		assertEquals(14, blocked.skillId)
		assertEquals(2, blocked.turnsRemainingBefore)
		assertEquals(35, resolved.participant("taunted")?.skillSlot(14)?.remainingPp)
		assertTrue(resolved.events.filterIsInstance<BattleEvent.SkillUsed>().isEmpty())
	}

	@Test
	fun `taunted participant can still use damaging skill`() {
		val scenario = publicBattleRuleScenario(
			name = "taunted-participant-can-use-damaging-skill",
			inputSummary = "处于挑衅的成员使用物理或特殊分类伤害技能。",
			expectedSummary = "挑衅不阻止攻击分类技能，技能正常消耗 PP 并造成伤害。",
		)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = fixedDamageSkill())
					.copy(tauntTurnsRemaining = 2),
				second = participant("target", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 49, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("taunted-participant-can-use-damaging-skill")
		assertEquals(80, resolved.participant("target")?.currentHp)
		assertEquals(34, resolved.participant("attacker")?.skillSlot(49)?.remainingPp)
		assertEquals(1, resolved.participant("attacker")?.tauntTurnsRemaining)
		assertEquals(20, resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount)
	}

	@Test
	fun `taunt clears when end turn duration reaches zero`() {
		val scenario = publicBattleRuleScenario(
			name = "taunt-clears-when-end-turn-duration-reaches-zero",
			inputSummary = "成员的挑衅只剩 1 回合，双方本回合没有行动。",
			expectedSummary = "回合末持续时间递减到 0，成员的挑衅被清除并产生临时状态解除事件。",
		)
		val state = engine.start(
			initialState(
				first = participant("taunted", speed = 100).copy(tauntTurnsRemaining = 1),
				second = participant("observer", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(state, emptyList(), ScriptedBattleRandom(emptyList()))

		scenario.assertNamed("taunt-clears-when-end-turn-duration-reaches-zero")
		assertEquals(0, resolved.participant("taunted")?.tauntTurnsRemaining)
		val cleared = resolved.events.filterIsInstance<BattleEvent.VolatileStatusCleared>().single()
		assertEquals("taunted", cleared.actorId)
		assertEquals(BattleVolatileStatus.TAUNT, cleared.status)
	}

	@Test
	fun `existing taunt blocks new taunt without refreshing duration`() {
		val scenario = publicBattleRuleScenario(
			name = "existing-taunt-blocks-new-taunt-without-refreshing-duration",
			inputSummary = "目标已经处于挑衅且剩余 2 回合，再次被挑衅类技能命中。",
			expectedSummary = "新挑衅不会刷新旧持续时间；当前回合结束后目标剩余 1 回合挑衅。",
		)
		val state = engine.start(
			initialState(
				first = participant("taunt-user", speed = 100, skill = tauntSkill()),
				second = participant("target", speed = 50).copy(tauntTurnsRemaining = 2),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("taunt-user", skillId = 269, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("existing-taunt-blocks-new-taunt-without-refreshing-duration")
		assertEquals(1, resolved.participant("target")?.tauntTurnsRemaining)
		val blocked = resolved.events.filterIsInstance<BattleEvent.VolatileStatusApplicationBlocked>().single()
		assertEquals(BattleVolatileStatus.TAUNT, blocked.status)
	}

	private fun tauntSkill() =
		damagingSkill(
			skillId = 269,
			name = "挑衅",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			volatileStatusApplications = listOf(
				BattleVolatileStatusApplication(
					status = BattleVolatileStatus.TAUNT,
					target = BattleEffectTarget.TARGET,
					chancePercent = 100,
				),
			),
		)

	private fun statusSkill() =
		damagingSkill(
			skillId = 14,
			name = "剑舞",
			damageClass = BattleDamageClass.STATUS,
			power = null,
		)

	private fun fixedDamageSkill() =
		damagingSkill(
			skillId = 49,
			name = "音爆",
			damageClass = BattleDamageClass.SPECIAL,
			power = null,
			fixedDamage = BattleFixedDamage.FixedAmount(20),
		)
}
