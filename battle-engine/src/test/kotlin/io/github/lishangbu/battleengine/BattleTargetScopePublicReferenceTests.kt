package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleInitialState
import io.github.lishangbu.battleengine.model.BattleSide
import io.github.lishangbu.battleengine.model.BattleSkillTargetScope
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证现代双打目标范围解析。
 *
 * 场景类型：目标选择、范围目标重新收集和倒下目标排除 场景。
 * 参考来源类型：公开成熟对战引擎的行动结算与技能目标资料。现代双打里，玩家提交的是技能和目标槽位，
 * 但技能真正执行时需要按当前站位重新计算实际目标：单体技能只影响选中槽位，范围技能会收集当前可战斗的
 * 相邻成员，主动替换先于普通攻击结算，已经倒下的成员不会继续作为范围目标参与命中和伤害。
 * 验证重点：目标范围不通过技能名称判断，而由 [BattleSkillTargetScope] 决定；目标列表会在技能执行时读取
 * 最新战斗状态；范围目标数量还会影响普通伤害公式中的目标倍率。
 */
class BattleTargetScopePublicReferenceTests {
	private val engine = BattleEngine()

	@Test
	fun `selected target skill hits only chosen target in double battle`() {
		val scenario = publicBattleRuleScenario(
			name = "selected-target-skill-hits-only-chosen-target-in-double-battle",
			inputSummary = "双打中使用普通单体技能，目标选择对方左侧成员。",
			expectedSummary = "技能只对选中目标产生一次伤害事件，同侧伙伴和另一名对手都不受影响。",
		)
		val state = engine.start(
			doubleInitialState(
				firstA = participant("attacker", speed = 100),
				firstB = participant("ally", speed = 90),
				secondA = participant("target-left", speed = 80),
				secondB = participant("target-right", speed = 70),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "target-left")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		scenario.assertNamed("selected-target-skill-hits-only-chosen-target-in-double-battle")
		assertEquals(listOf("target-left"), resolved.events.filterIsInstance<BattleEvent.DamageApplied>().map { it.targetActorId })
		assertEquals(72, resolved.participant("target-left")?.currentHp)
		assertEquals(100, resolved.participant("target-right")?.currentHp)
		assertEquals(100, resolved.participant("ally")?.currentHp)
	}

	@Test
	fun `all adjacent opponents skill hits both opponents and skips ally`() {
		val scenario = publicBattleRuleScenario(
			name = "all-adjacent-opponents-skill-hits-both-opponents-and-skips-ally",
			inputSummary = "双打中使用目标范围为全体相邻对手的伤害技能，对方两名成员均可战斗。",
			expectedSummary = "技能对两个对手各产生一次伤害事件，不影响使用者同侧伙伴，并使用范围伤害倍率。",
		)
		val spreadSkill = damagingSkill(targetScope = BattleSkillTargetScope.ALL_ADJACENT_OPPONENTS)
		val state = engine.start(
			doubleInitialState(
				firstA = participant("spread-user", speed = 100, skill = spreadSkill),
				firstB = participant("ally", speed = 90),
				secondA = participant("opponent-left", speed = 80),
				secondB = participant("opponent-right", speed = 70),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("spread-user", skillId = 1, targetActorId = "opponent-left")),
			ScriptedBattleRandom(listOf(1, 15, 1, 15)),
		)
		val damageEvents = resolved.events.filterIsInstance<BattleEvent.DamageApplied>()

		scenario.assertNamed("all-adjacent-opponents-skill-hits-both-opponents-and-skips-ally")
		assertEquals(listOf("opponent-left", "opponent-right"), damageEvents.map { it.targetActorId })
		assertEquals(listOf(0.75, 0.75), damageEvents.map { it.targetMultiplier })
		assertEquals(100, resolved.participant("ally")?.currentHp)
	}

	@Test
	fun `all adjacent participants skill hits ally and opponents but excludes user`() {
		val scenario = publicBattleRuleScenario(
			name = "all-adjacent-participants-skill-hits-ally-and-opponents-but-excludes-user",
			inputSummary = "双打中使用目标范围为全体相邻成员的伤害技能，使用者伙伴和两个对手都可战斗。",
			expectedSummary = "技能命中除使用者外的三个相邻成员，使用者自身不受到该范围技能影响。",
		)
		val earthquakeLikeSkill = damagingSkill(targetScope = BattleSkillTargetScope.ALL_ADJACENT_PARTICIPANTS)
		val state = engine.start(
			doubleInitialState(
				firstA = participant("area-user", speed = 100, skill = earthquakeLikeSkill),
				firstB = participant("ally", speed = 90),
				secondA = participant("opponent-left", speed = 80),
				secondB = participant("opponent-right", speed = 70),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("area-user", skillId = 1, targetActorId = "opponent-left")),
			ScriptedBattleRandom(listOf(1, 15, 1, 15, 1, 15)),
		)
		val damageEvents = resolved.events.filterIsInstance<BattleEvent.DamageApplied>()

		scenario.assertNamed("all-adjacent-participants-skill-hits-ally-and-opponents-but-excludes-user")
		assertEquals(listOf("ally", "opponent-left", "opponent-right"), damageEvents.map { it.targetActorId })
		assertEquals(100, resolved.participant("area-user")?.currentHp)
		assertEquals(79, resolved.participant("ally")?.currentHp)
	}

	@Test
	fun `spread skill skips fainted opponent and keeps full target multiplier`() {
		val scenario = publicBattleRuleScenario(
			name = "spread-skill-skips-fainted-opponent-and-keeps-full-target-multiplier",
			inputSummary = "双打中使用全体相邻对手范围技能，但其中一个对手已经无法战斗。",
			expectedSummary = "范围目标列表只包含仍可战斗的对手，普通伤害公式使用 1.0 目标倍率。",
		)
		val spreadSkill = damagingSkill(targetScope = BattleSkillTargetScope.ALL_ADJACENT_OPPONENTS)
		val state = engine.start(
			doubleInitialState(
				firstA = participant("spread-user", speed = 100, skill = spreadSkill),
				firstB = participant("ally", speed = 90),
				secondA = participant("opponent-left", speed = 80),
				secondB = participant("opponent-right", speed = 70, currentHp = 0),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("spread-user", skillId = 1, targetActorId = "opponent-left")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val damageEvent = resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single()

		scenario.assertNamed("spread-skill-skips-fainted-opponent-and-keeps-full-target-multiplier")
		assertEquals("opponent-left", damageEvent.targetActorId)
		assertEquals(1.0, damageEvent.targetMultiplier)
		assertEquals(28, damageEvent.amount)
	}

	@Test
	fun `spread skill recalculates opponents after voluntary switch`() {
		val scenario = publicBattleRuleScenario(
			name = "spread-skill-recalculates-opponents-after-voluntary-switch",
			inputSummary = "双打中对手左侧成员先主动替换，随后另一方使用全体相邻对手范围技能。",
			expectedSummary = "范围技能按替换后的当前站位重新收集目标，命中新上场成员和另一名仍在场对手。",
		)
		val spreadSkill = damagingSkill(targetScope = BattleSkillTargetScope.ALL_ADJACENT_OPPONENTS)
		val state = engine.start(
			BattleInitialState(
				format = doubleFormat(),
				rules = neutralRules(),
				sides = listOf(
					BattleSide(
						sideId = "side-a",
						activeActorIds = listOf("spread-user", "ally"),
						participants = listOf(
							participant("spread-user", speed = 100, skill = spreadSkill),
							participant("ally", speed = 90),
						),
					),
					BattleSide(
						sideId = "side-b",
						activeActorIds = listOf("opponent-left", "opponent-right"),
						participants = listOf(
							participant("opponent-left", speed = 80),
							participant("opponent-right", speed = 70),
							participant("reserve", speed = 60),
						),
					),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.SwitchParticipant("opponent-left", targetActorId = "reserve"),
				BattleAction.UseSkill("spread-user", skillId = 1, targetActorId = "opponent-left"),
			),
			ScriptedBattleRandom(listOf(1, 15, 1, 15)),
		)
		val damageEvents = resolved.events.filterIsInstance<BattleEvent.DamageApplied>()

		scenario.assertNamed("spread-skill-recalculates-opponents-after-voluntary-switch")
		assertEquals(listOf("reserve", "opponent-right"), damageEvents.map { it.targetActorId })
		assertEquals(100, resolved.participant("opponent-left")?.currentHp)
		assertEquals(79, resolved.participant("reserve")?.currentHp)
	}

	@Test
	fun `all adjacent participants skill skips fainted ally`() {
		val scenario = publicBattleRuleScenario(
			name = "all-adjacent-participants-skill-skips-fainted-ally",
			inputSummary = "双打中使用全体相邻成员范围技能，使用者伙伴已经无法战斗，两个对手仍可战斗。",
			expectedSummary = "范围目标列表跳过已经无法战斗的伙伴，只命中两个仍可战斗的对手。",
		)
		val areaSkill = damagingSkill(targetScope = BattleSkillTargetScope.ALL_ADJACENT_PARTICIPANTS)
		val state = engine.start(
			doubleInitialState(
				firstA = participant("area-user", speed = 100, skill = areaSkill),
				firstB = participant("fainted-ally", speed = 90, currentHp = 0),
				secondA = participant("opponent-left", speed = 80),
				secondB = participant("opponent-right", speed = 70),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("area-user", skillId = 1, targetActorId = "opponent-left")),
			ScriptedBattleRandom(listOf(1, 15, 1, 15)),
		)
		val damageEvents = resolved.events.filterIsInstance<BattleEvent.DamageApplied>()

		scenario.assertNamed("all-adjacent-participants-skill-skips-fainted-ally")
		assertEquals(listOf("opponent-left", "opponent-right"), damageEvents.map { it.targetActorId })
		assertEquals(0, resolved.participant("fainted-ally")?.currentHp)
		assertEquals(listOf(0.75, 0.75), damageEvents.map { it.targetMultiplier })
	}
}
