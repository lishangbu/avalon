package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.SkillPreventionReason
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleSkillHpEffect
import io.github.lishangbu.battleengine.model.BattleVolatileStatus
import io.github.lishangbu.battleengine.model.BattleVolatileStatusApplication
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 验证回复封锁这一临时状态的现代主系列规则。
 *
 * 场景类型：技能临时状态与 HP 回复阻止 fixture。
 * 参考来源类型：本地中文资料集对现代效果的整理，以及公开成熟模拟器中的技能效果声明。第六世代以后，
 * 处于回复封锁的成员不能使用主动回复技能和吸取回复类技能；其它被动回复来源也不能让 HP 上升。
 * 验证重点：回复封锁作为可持续临时状态写入目标，按回合末递减并自然解除；被封锁成员的回复类技能在 PP
 * 消耗前失败；已有回合末道具回复入口不会绕过该状态。
 */
class BattleHealBlockTests {
	private val engine = BattleEngine()

	@Test
	fun `status skill applies heal block to target for five turns`() {
		val fixture = publicBattleRuleFixture(
			name = "status-skill-applies-heal-block-to-target",
			inputSummary = "使用者用回复封锁类变化技能命中目标。",
			expectedSummary = "目标获得回复封锁临时状态；当前回合结束后剩余 4 回合。",
		)
		val state = engine.start(
			initialState(
				first = participant("block-user", speed = 100, skill = healBlockSkill()),
				second = participant("target", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("block-user", skillId = 377, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("status-skill-applies-heal-block-to-target")
		val applied = resolved.events.filterIsInstance<BattleEvent.VolatileStatusApplied>().single()
		assertEquals(BattleVolatileStatus.HEAL_BLOCK, applied.status)
		assertEquals("target", applied.targetActorId)
		assertEquals(4, resolved.participant("target")?.healBlockTurnsRemaining)
	}

	@Test
	fun `heal blocked participant cannot use self healing status skill`() {
		val fixture = publicBattleRuleFixture(
			name = "heal-blocked-participant-cannot-use-self-healing-status-skill",
			inputSummary = "处于回复封锁且未满 HP 的成员尝试使用自我回复变化技能。",
			expectedSummary = "技能在 PP 消耗前失败，成员 HP 不变，并产生回复封锁阻止事件。",
		)
		val state = engine.start(
			initialState(
				first = participant("healer", speed = 100, currentHp = 30, skill = recoverSkill())
					.copy(healBlockTurnsRemaining = 2),
				second = participant("observer", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("healer", skillId = 105, targetActorId = "healer")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("heal-blocked-participant-cannot-use-self-healing-status-skill")
		val blocked = resolved.events.filterIsInstance<BattleEvent.SkillPrevented>().filter { it.reason == SkillPreventionReason.HEAL_BLOCK }.single()
		assertEquals("healer", blocked.actorId)
		assertEquals(105, blocked.skillId)
		assertEquals(2, blocked.turnsRemainingBefore)
		assertEquals(30, resolved.participant("healer")?.currentHp)
		assertEquals(35, resolved.participant("healer")?.skillSlot(105)?.remainingPp)
		assertTrue(resolved.events.filterIsInstance<BattleEvent.SkillHealingApplied>().isEmpty())
	}

	@Test
	fun `heal blocked participant cannot use draining damage skill`() {
		val fixture = publicBattleRuleFixture(
			name = "heal-blocked-participant-cannot-use-draining-damage-skill",
			inputSummary = "处于回复封锁且未满 HP 的成员尝试使用吸取回复类伤害技能。",
			expectedSummary = "吸取类技能在 PP 消耗前失败，目标不受伤害，使用者也不会回复 HP。",
		)
		val state = engine.start(
			initialState(
				first = participant("drain-user", speed = 100, currentHp = 20, skill = drainSkill())
					.copy(healBlockTurnsRemaining = 2),
				second = participant("target", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("drain-user", skillId = 71, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("heal-blocked-participant-cannot-use-draining-damage-skill")
		assertEquals(20, resolved.participant("drain-user")?.currentHp)
		assertEquals(100, resolved.participant("target")?.currentHp)
		assertEquals(35, resolved.participant("drain-user")?.skillSlot(71)?.remainingPp)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.DamageApplied>())
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillHealingApplied>())
	}

	@Test
	fun `heal block suppresses end turn held item healing`() {
		val fixture = publicBattleRuleFixture(
			name = "heal-block-suppresses-end-turn-held-item-healing",
			inputSummary = "处于回复封锁且未满 HP 的成员携带回合末固定回复道具。",
			expectedSummary = "回合末不会回复 HP，也不会产生通用回复事件；回复封锁持续时间正常递减。",
		)
		val state = engine.start(
			initialState(
				first = participant(
					"item-healer",
					speed = 100,
					currentHp = 40,
					itemId = 1,
					itemEffects = listOf(BattleItemEffect.HeldEndTurnHeal(healDenominator = 16)),
				).copy(healBlockTurnsRemaining = 2),
				second = participant("observer", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(state, emptyList(), ScriptedBattleRandom(emptyList()))

		fixture.assertNamed("heal-block-suppresses-end-turn-held-item-healing")
		assertEquals(40, resolved.participant("item-healer")?.currentHp)
		assertEquals(1, resolved.participant("item-healer")?.healBlockTurnsRemaining)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.HealingApplied>())
	}

	@Test
	fun `heal block clears when end turn duration reaches zero`() {
		val fixture = publicBattleRuleFixture(
			name = "heal-block-clears-when-end-turn-duration-reaches-zero",
			inputSummary = "成员的回复封锁只剩 1 回合，双方本回合没有行动。",
			expectedSummary = "回合末持续时间递减到 0，成员的回复封锁被清除并产生临时状态解除事件。",
		)
		val state = engine.start(
			initialState(
				first = participant("blocked", speed = 100).copy(healBlockTurnsRemaining = 1),
				second = participant("observer", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(state, emptyList(), ScriptedBattleRandom(emptyList()))

		fixture.assertNamed("heal-block-clears-when-end-turn-duration-reaches-zero")
		assertEquals(0, resolved.participant("blocked")?.healBlockTurnsRemaining)
		val cleared = resolved.events.filterIsInstance<BattleEvent.VolatileStatusCleared>().single()
		assertEquals("blocked", cleared.actorId)
		assertEquals(BattleVolatileStatus.HEAL_BLOCK, cleared.status)
	}

	private fun healBlockSkill() =
		damagingSkill(
			skillId = 377,
			name = "回复封锁",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			volatileStatusApplications = listOf(
				BattleVolatileStatusApplication(
					status = BattleVolatileStatus.HEAL_BLOCK,
					target = BattleEffectTarget.TARGET,
					chancePercent = 100,
				),
			),
		)

	private fun recoverSkill() =
		damagingSkill(
			skillId = 105,
			name = "自我再生",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			hpEffects = listOf(BattleSkillHpEffect.SelfHealMaxHpFraction(numerator = 1, denominator = 2)),
		)

	private fun drainSkill() =
		damagingSkill(
			skillId = 71,
			name = "吸取",
			hpEffects = listOf(BattleSkillHpEffect.DrainDamage(numerator = 1, denominator = 2)),
		)
}
