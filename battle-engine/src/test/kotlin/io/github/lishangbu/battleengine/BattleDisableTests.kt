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
 * 验证定身法这一临时状态的现代主系列规则。
 *
 * 场景类型：技能临时状态与行动选择限制 fixture。
 * 参考来源类型：公开成熟模拟器中的技能资料和状态条件说明，以及中文公开规则资料。现代规则中，定身法会读取
 * 目标最近一次成功使用的技能，并在固定持续回合内禁止目标再次使用该技能；其它技能不受影响。如果目标没有
 * 可被禁用的最近使用技能，定身法不会写入状态。
 * 验证重点：定身法能记录被禁用技能 ID；被禁用技能在 PP 消耗前失败；非禁用技能继续结算；持续回合在回合末
 * 递减并自然解除；没有最近使用技能时稳定失败。
 */
class BattleDisableTests {
	private val engine = BattleEngine()

	@Test
	fun `status skill disables target last used skill for four turns`() {
		val fixture = publicBattleRuleFixture(
			name = "status-skill-disables-target-last-used-skill",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
				"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
				"https://wiki.52poke.com/wiki/定身法（招式）",
			),
			inputSummary = "目标最近一次成功使用过仍有 PP 的技能，使用者用定身法命中目标。",
			expectedSummary = "目标最近使用的技能被禁用 4 回合；当前回合结束后剩余 3 回合。",
		)
		val state = engine.start(
			initialState(
				first = participant("disable-user", speed = 100, skill = disableSkill()),
				second = participant("target", speed = 50, skill = fixedDamageSkill(1)).copy(lastSuccessfulSkillId = 1),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("disable-user", skillId = 50, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("status-skill-disables-target-last-used-skill")
		val applied = resolved.events.filterIsInstance<BattleEvent.VolatileStatusApplied>().single()
		assertEquals(BattleVolatileStatus.DISABLE, applied.status)
		val disabled = resolved.events.filterIsInstance<BattleEvent.SkillDisabled>().single()
		assertEquals("target", disabled.targetActorId)
		assertEquals(1, disabled.disabledSkillId)
		assertEquals(4, disabled.turnsRemaining)
		assertEquals(1, resolved.participant("target")?.disabledSkillId)
		assertEquals(3, resolved.participant("target")?.disabledSkillTurnsRemaining)
	}

	@Test
	fun `disabled skill cannot be used`() {
		val fixture = publicBattleRuleFixture(
			name = "disabled-skill-cannot-be-used",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
				"https://wiki.52poke.com/wiki/定身法（招式）",
			),
			inputSummary = "成员处于定身法状态，并尝试使用被禁用的技能。",
			expectedSummary = "被禁用技能在 PP 消耗前失败，成员不会产生技能使用事件。",
		)
		val state = engine.start(
			initialState(
				first = participant("disabled", speed = 100, skill = fixedDamageSkill(1))
					.copy(disabledSkillId = 1, disabledSkillTurnsRemaining = 2),
				second = participant("target", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("disabled", skillId = 1, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("disabled-skill-cannot-be-used")
		val blocked = resolved.events.filterIsInstance<BattleEvent.SkillPreventedByDisable>().single()
		assertEquals("disabled", blocked.actorId)
		assertEquals(1, blocked.skillId)
		assertEquals(2, blocked.turnsRemainingBefore)
		assertEquals(35, resolved.participant("disabled")?.skillSlot(1)?.remainingPp)
		assertEquals(1, resolved.participant("disabled")?.disabledSkillTurnsRemaining)
		assertTrue(resolved.events.filterIsInstance<BattleEvent.SkillUsed>().isEmpty())
	}

	@Test
	fun `disabled participant can use other skill`() {
		val fixture = publicBattleRuleFixture(
			name = "disabled-participant-can-use-other-skill",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
				"https://wiki.52poke.com/wiki/定身法（招式）",
			),
			inputSummary = "成员处于定身法状态，但选择了未被禁用的其它技能。",
			expectedSummary = "未被禁用的技能正常消耗 PP 并造成伤害，定身法持续回合照常递减。",
		)
		val disabledSkill = fixedDamageSkill(1)
		val otherSkill = fixedDamageSkill(2)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = disabledSkill).copy(
					skillSlots = listOf(disabledSkill, otherSkill),
					disabledSkillId = 1,
					disabledSkillTurnsRemaining = 2,
				),
				second = participant("target", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 2, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("disabled-participant-can-use-other-skill")
		assertEquals(80, resolved.participant("target")?.currentHp)
		assertEquals(34, resolved.participant("attacker")?.skillSlot(2)?.remainingPp)
		assertEquals(1, resolved.participant("attacker")?.disabledSkillId)
		assertEquals(1, resolved.participant("attacker")?.disabledSkillTurnsRemaining)
		assertEquals(20, resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount)
	}

	@Test
	fun `disable clears when end turn duration reaches zero`() {
		val fixture = publicBattleRuleFixture(
			name = "disable-clears-when-end-turn-duration-reaches-zero",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
				"https://wiki.52poke.com/wiki/定身法（招式）",
			),
			inputSummary = "成员的定身法只剩 1 回合，双方本回合没有行动。",
			expectedSummary = "回合末持续时间递减到 0，成员的定身法被清除并产生临时状态解除事件。",
		)
		val state = engine.start(
			initialState(
				first = participant("disabled", speed = 100).copy(
					disabledSkillId = 1,
					disabledSkillTurnsRemaining = 1,
				),
				second = participant("observer", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(state, emptyList(), ScriptedBattleRandom(emptyList()))

		fixture.assertNamed("disable-clears-when-end-turn-duration-reaches-zero")
		assertEquals(null, resolved.participant("disabled")?.disabledSkillId)
		assertEquals(0, resolved.participant("disabled")?.disabledSkillTurnsRemaining)
		val cleared = resolved.events.filterIsInstance<BattleEvent.VolatileStatusCleared>().single()
		assertEquals("disabled", cleared.actorId)
		assertEquals(BattleVolatileStatus.DISABLE, cleared.status)
	}

	@Test
	fun `disable fails when target has no last used skill`() {
		val fixture = publicBattleRuleFixture(
			name = "disable-fails-when-target-has-no-last-used-skill",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
				"https://wiki.52poke.com/wiki/定身法（招式）",
			),
			inputSummary = "目标自上场以来还没有成功使用过技能，使用者尝试用定身法影响目标。",
			expectedSummary = "定身法不会写入临时状态，并以没有可禁用技能作为稳定失败原因。",
		)
		val state = engine.start(
			initialState(
				first = participant("disable-user", speed = 100, skill = disableSkill()),
				second = participant("target", speed = 50, skill = fixedDamageSkill(1)),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("disable-user", skillId = 50, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("disable-fails-when-target-has-no-last-used-skill")
		val blocked = resolved.events.filterIsInstance<BattleEvent.VolatileStatusApplicationBlocked>().single()
		assertEquals(BattleVolatileStatus.DISABLE, blocked.status)
		assertEquals(BattleStatusBlockReason.NO_ELIGIBLE_SKILL, blocked.reason)
		assertEquals(null, resolved.participant("target")?.disabledSkillId)
		assertEquals(0, resolved.participant("target")?.disabledSkillTurnsRemaining)
	}

	private fun disableSkill() =
		damagingSkill(
			skillId = 50,
			name = "定身法",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			volatileStatusApplications = listOf(
				BattleVolatileStatusApplication(
					status = BattleVolatileStatus.DISABLE,
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
