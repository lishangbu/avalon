package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleFixedDamage
import io.github.lishangbu.battleengine.model.BattleStatusBlockReason
import io.github.lishangbu.battleengine.model.BattleVolatileStatus
import io.github.lishangbu.battleengine.model.BattleVolatileStatusApplication
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 验证无理取闹这一临时状态的现代主系列规则。
 *
 * 场景类型：技能临时状态与行动选择限制 fixture。
 * 参考来源类型：公开成熟模拟器中的技能资料和状态条件说明，以及中文公开规则资料。现代规则中，无理取闹会让
 * 目标无法连续两次真正使用同一个技能；目标改用其它技能后，最近成功技能会更新，下一次限制也随之移动。
 * 该状态没有固定回合倒计时，离开上场席位时结束，已有无理取闹不会被重复附加刷新。
 * 验证重点：无理取闹能写入目标；连续使用同一技能在 PP 消耗前失败；不同技能继续结算并更新最近技能；
 * 离场会清除无理取闹和最近技能；重复附加会稳定报告已有临时状态。
 */
class BattleTormentTests {
	private val engine = BattleEngine()

	@Test
	fun `status skill applies torment to target until switch out`() {
		val fixture = publicBattleRuleFixture(
			name = "status-skill-applies-torment-to-target",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
				"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
				"https://wiki.52poke.com/wiki/无理取闹（招式）",
			),
			inputSummary = "使用者用无理取闹类变化技能命中目标。",
			expectedSummary = "目标获得无理取闹临时状态；回合末不会按倒计时自然递减。",
		)
		val state = engine.start(
			initialState(
				first = participant("torment-user", speed = 100, skill = tormentSkill()),
				second = participant("target", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("torment-user", skillId = 259, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("status-skill-applies-torment-to-target")
		val applied = resolved.events.filterIsInstance<BattleEvent.VolatileStatusApplied>().single()
		assertEquals(BattleVolatileStatus.TORMENT, applied.status)
		assertEquals("target", applied.targetActorId)
		assertEquals(true, resolved.participant("target")?.tormented)
		assertTrue(resolved.events.filterIsInstance<BattleEvent.VolatileStatusCleared>().isEmpty())
	}

	@Test
	fun `tormented participant cannot use same skill twice in a row`() {
		val fixture = publicBattleRuleFixture(
			name = "tormented-participant-cannot-use-same-skill-twice",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
				"https://wiki.52poke.com/wiki/无理取闹（状态变化）",
			),
			inputSummary = "成员处于无理取闹状态，上一回合成功使用过技能 1，本回合再次尝试技能 1。",
			expectedSummary = "重复技能在 PP 消耗前失败，成员不会产生技能使用事件，最近成功技能保持不变。",
		)
		val state = engine.start(
			initialState(
				first = participant("tormented", speed = 100, skill = fixedDamageSkill(1))
					.copy(tormented = true, lastSuccessfulSkillId = 1),
				second = participant("target", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("tormented", skillId = 1, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("tormented-participant-cannot-use-same-skill-twice")
		val blocked = resolved.events.filterIsInstance<BattleEvent.SkillPreventedByTorment>().single()
		assertEquals("tormented", blocked.actorId)
		assertEquals(1, blocked.skillId)
		assertEquals(1, blocked.previousSkillId)
		assertEquals(35, resolved.participant("tormented")?.skillSlot(1)?.remainingPp)
		assertEquals(1, resolved.participant("tormented")?.lastSuccessfulSkillId)
		assertTrue(resolved.events.filterIsInstance<BattleEvent.SkillUsed>().isEmpty())
	}

	@Test
	fun `tormented participant can use a different skill`() {
		val fixture = publicBattleRuleFixture(
			name = "tormented-participant-can-use-different-skill",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
				"https://wiki.52poke.com/wiki/无理取闹（状态变化）",
			),
			inputSummary = "成员处于无理取闹状态，上一回合成功使用过技能 1，本回合选择技能 2。",
			expectedSummary = "不同技能正常消耗 PP 并造成伤害，最近成功技能更新为技能 2。",
		)
		val firstSkill = fixedDamageSkill(1)
		val secondSkill = fixedDamageSkill(2)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = firstSkill).copy(
					skillSlots = listOf(firstSkill, secondSkill),
					tormented = true,
					lastSuccessfulSkillId = 1,
				),
				second = participant("target", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 2, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("tormented-participant-can-use-different-skill")
		assertEquals(80, resolved.participant("target")?.currentHp)
		assertEquals(34, resolved.participant("attacker")?.skillSlot(2)?.remainingPp)
		assertEquals(true, resolved.participant("attacker")?.tormented)
		assertEquals(2, resolved.participant("attacker")?.lastSuccessfulSkillId)
		assertEquals(20, resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount)
	}

	@Test
	fun `torment clears when participant switches out`() {
		val fixture = publicBattleRuleFixture(
			name = "torment-clears-when-participant-switches-out",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
				"https://wiki.52poke.com/wiki/无理取闹（状态变化）",
			),
			inputSummary = "处于无理取闹状态的成员主动替换离场。",
			expectedSummary = "离场成员的无理取闹和最近成功技能被清除，HP、PP 和其它持久资料保持。",
		)
		val state = engine.start(
			initialState(
				first = participant("starter", speed = 100).copy(tormented = true, lastSuccessfulSkillId = 1),
				firstBench = listOf(participant("reserve", speed = 80)),
				second = participant("observer", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.SwitchParticipant("starter", targetActorId = "reserve")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("torment-clears-when-participant-switches-out")
		val switchedOut = resolved.participant("starter")
		assertEquals(false, switchedOut?.tormented)
		assertEquals(null, switchedOut?.lastSuccessfulSkillId)
		assertEquals(35, switchedOut?.skillSlot(1)?.remainingPp)
	}

	@Test
	fun `existing torment blocks new torment without refreshing state`() {
		val fixture = publicBattleRuleFixture(
			name = "existing-torment-blocks-new-torment-without-refreshing-state",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
				"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
			),
			inputSummary = "目标已经处于无理取闹，再次被无理取闹类技能命中。",
			expectedSummary = "新无理取闹不会改变目标运行态，并以已有临时状态作为稳定失败原因。",
		)
		val state = engine.start(
			initialState(
				first = participant("torment-user", speed = 100, skill = tormentSkill()),
				second = participant("target", speed = 50).copy(tormented = true, lastSuccessfulSkillId = 1),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("torment-user", skillId = 259, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("existing-torment-blocks-new-torment-without-refreshing-state")
		val blocked = resolved.events.filterIsInstance<BattleEvent.VolatileStatusApplicationBlocked>().single()
		assertEquals(BattleVolatileStatus.TORMENT, blocked.status)
		assertEquals(BattleStatusBlockReason.EXISTING_STATUS, blocked.reason)
		assertEquals(true, resolved.participant("target")?.tormented)
		assertEquals(1, resolved.participant("target")?.lastSuccessfulSkillId)
	}

	private fun tormentSkill() =
		damagingSkill(
			skillId = 259,
			name = "无理取闹",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			volatileStatusApplications = listOf(
				BattleVolatileStatusApplication(
					status = BattleVolatileStatus.TORMENT,
					target = BattleEffectTarget.TARGET,
					chancePercent = 100,
				),
			),
		)

	private fun fixedDamageSkill(skillId: Long) =
		damagingSkill(
			skillId = skillId,
			name = "固定伤害$skillId",
			damageClass = BattleDamageClass.SPECIAL,
			power = null,
			fixedDamage = BattleFixedDamage.FixedAmount(20),
		)
}
