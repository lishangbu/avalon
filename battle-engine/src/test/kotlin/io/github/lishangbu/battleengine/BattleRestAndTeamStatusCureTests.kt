package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleSkillHpEffect
import io.github.lishangbu.battleengine.model.BattleSkillTargetScope
import io.github.lishangbu.battleengine.model.BattleStatusBlockReason
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证睡觉和治愈铃声这组“状态治愈 + 回复/队伍范围”规则。
 *
 * 场景类型：变化技能成功后的主要异常状态重写与清除。参考来源类型：公开资料说明和成熟公开对战引擎行为。
 * 现代规则下，睡觉会在使用者 HP 不满且可以入睡时写入固定睡眠并回满 HP，旧主要异常会被睡眠替换；治愈铃声
 * 清除使用者整侧所有成员的主要异常状态，但不清除混乱等临时状态。测试直接断言事件顺序、最终 HP、睡眠计数
 * 和随机数消费，避免这类技能被误接到普通随机睡眠或普通单体治疗路径。
 */
class BattleRestAndTeamStatusCureTests {
	private val engine = BattleEngine()

	@Test
	fun `rest replaces existing major status with fixed sleep and fully heals user`() {
		val scenario = publicBattleRuleScenario(
			name = "rest-replaces-existing-major-status-with-fixed-sleep-and-fully-heals-user",
			inputSummary = "受伤且灼伤的使用者成功使用睡觉。",
			expectedSummary = "旧主要异常先被清除，使用者进入固定 2 次行动阻止的睡眠，并回复到满 HP。",
		)
		val user = participant("rest-user", speed = 100, currentHp = 31, skill = restSkill())
			.copy(majorStatus = BattleMajorStatus.BURN)
		val random = ScriptedBattleRandom(emptyList())

		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = user,
					second = participant("observer", speed = 50),
				),
			),
			listOf(BattleAction.UseSkill("rest-user", skillId = 156, targetActorId = "rest-user")),
			random,
		)

		scenario.assertNamed("rest-replaces-existing-major-status-with-fixed-sleep-and-fully-heals-user")
		assertEquals(BattleMajorStatus.SLEEP, resolved.participant("rest-user")?.majorStatus)
		assertEquals(2, resolved.participant("rest-user")?.sleepTurnsRemaining)
		assertEquals(100, resolved.participant("rest-user")?.currentHp)
		assertEquals(BattleMajorStatus.BURN, resolved.events.filterIsInstance<BattleEvent.StatusCleared>().single().status)
		assertEquals(BattleMajorStatus.SLEEP, resolved.events.filterIsInstance<BattleEvent.StatusApplied>().single().status)
		assertEquals(69, resolved.events.filterIsInstance<BattleEvent.SkillHealingApplied>().single().amount)
		assertEquals(emptyList(), random.consumedReasons())
	}

	@Test
	fun `rest is blocked by terrain sleep immunity before healing`() {
		val scenario = publicBattleRuleScenario(
			name = "rest-is-blocked-by-terrain-sleep-immunity-before-healing",
			inputSummary = "受伤且接地的使用者在电气场地下尝试睡觉。",
			expectedSummary = "电气场地阻止使用者获得睡眠，技能不回复 HP，也不消费睡眠随机数。",
		)
		val random = ScriptedBattleRandom(emptyList())

		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("rest-user", speed = 100, currentHp = 40, skill = restSkill()),
					second = participant("observer", speed = 50),
					environment = BattleEnvironment(terrain = BattleTerrain.ELECTRIC, terrainTurnsRemaining = 5),
				),
			),
			listOf(BattleAction.UseSkill("rest-user", skillId = 156, targetActorId = "rest-user")),
			random,
		)

		scenario.assertNamed("rest-is-blocked-by-terrain-sleep-immunity-before-healing")
		assertEquals(null, resolved.participant("rest-user")?.majorStatus)
		assertEquals(40, resolved.participant("rest-user")?.currentHp)
		assertEquals(BattleStatusBlockReason.TERRAIN, resolved.events.filterIsInstance<BattleEvent.StatusApplicationBlocked>().single().reason)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillHealingApplied>())
		assertEquals(emptyList(), random.consumedReasons())
	}

	@Test
	fun `heal bell clears user side major statuses but keeps volatile confusion`() {
		val scenario = publicBattleRuleScenario(
			name = "heal-bell-clears-user-side-major-statuses-but-keeps-volatile-confusion",
			inputSummary = "使用者一侧上场成员和后备成员都有主要异常，其中后备成员还处于混乱。",
			expectedSummary = "使用者一侧所有主要异常被清除，对手状态不受影响，混乱这类临时状态不会被清除。",
		)
		val active = participant("bell-user", speed = 100, skill = healBellSkill())
			.copy(majorStatus = BattleMajorStatus.BURN)
		val benched = participant("benched-ally", speed = 70)
			.copy(majorStatus = BattleMajorStatus.POISON, confusionTurnsRemaining = 3)
		val opponent = participant("opponent", speed = 50)
			.copy(majorStatus = BattleMajorStatus.BURN)

		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = active,
					firstBench = listOf(benched),
					second = opponent,
				),
			),
			listOf(BattleAction.UseSkill("bell-user", skillId = 215, targetActorId = "bell-user")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("heal-bell-clears-user-side-major-statuses-but-keeps-volatile-confusion")
		assertEquals(null, resolved.participant("bell-user")?.majorStatus)
		assertEquals(null, resolved.participant("benched-ally")?.majorStatus)
		assertEquals(3, resolved.participant("benched-ally")?.confusionTurnsRemaining)
		assertEquals(BattleMajorStatus.BURN, resolved.participant("opponent")?.majorStatus)
		assertEquals(
			listOf("bell-user", "benched-ally"),
			resolved.events.filterIsInstance<BattleEvent.StatusCleared>().map { it.actorId },
		)
	}

	@Test
	fun `life dew heals user side active participants by quarter max hp`() {
		val scenario = publicBattleRuleScenario(
			name = "life-dew-heals-user-side-active-participants-by-quarter-max-hp",
			inputSummary = "双打中使用者和同侧上场同伴都受伤，使用者成功使用生命水滴。",
			expectedSummary = "只有使用者同侧当前上场成员各按最大 HP 的 1/4 回复，对手一侧不受影响。",
		)
		val user = participant("dew-user", speed = 100, currentHp = 40, skill = lifeDewSkill())
		val ally = participant("dew-ally", speed = 90, currentHp = 70)
		val opponentA = participant("opponent-a", speed = 80, currentHp = 50)
		val opponentB = participant("opponent-b", speed = 70, currentHp = 60)

		val resolved = engine.resolveTurn(
			engine.start(
				doubleInitialState(
					firstA = user,
					firstB = ally,
					secondA = opponentA,
					secondB = opponentB,
				),
			),
			listOf(BattleAction.UseSkill("dew-user", skillId = 791, targetActorId = "dew-user")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("life-dew-heals-user-side-active-participants-by-quarter-max-hp")
		assertEquals(65, resolved.participant("dew-user")?.currentHp)
		assertEquals(95, resolved.participant("dew-ally")?.currentHp)
		assertEquals(50, resolved.participant("opponent-a")?.currentHp)
		assertEquals(60, resolved.participant("opponent-b")?.currentHp)
		assertEquals(
			listOf("dew-user" to 25, "dew-ally" to 25),
			resolved.events.filterIsInstance<BattleEvent.SkillHealingApplied>().map { it.actorId to it.amount },
		)
	}

	@Test
	fun `active side healing cure affects active allies but leaves bench and opponents unchanged`() {
		val scenario = publicBattleRuleScenario(
			name = "active-side-healing-cure-affects-active-allies-but-leaves-bench-and-opponents-unchanged",
			inputSummary = "使用者一侧当前上场成员、后备成员和对手都带有主要异常，使用者成功使用同侧上场回复净化技能。",
			expectedSummary = "只有使用者同侧当前上场成员回复并清除主要异常，后备成员和对手仍保留原主要异常。",
		)
		val user = participant("blessing-user", speed = 100, currentHp = 40, skill = lunarBlessingSkill())
			.copy(majorStatus = BattleMajorStatus.BURN)
		val ally = participant("blessing-ally", speed = 90, currentHp = 80)
			.copy(majorStatus = BattleMajorStatus.POISON)
		val benched = participant("benched-ally", speed = 60, currentHp = 50)
			.copy(majorStatus = BattleMajorStatus.PARALYSIS)
		val opponentA = participant("opponent-a", speed = 80, currentHp = 70)
			.copy(majorStatus = BattleMajorStatus.BURN)
		val opponentB = participant("opponent-b", speed = 70, currentHp = 70)
			.copy(majorStatus = BattleMajorStatus.FREEZE)
		val initial = doubleInitialState(
			firstA = user,
			firstB = ally,
			secondA = opponentA,
			secondB = opponentB,
		)

		val resolved = engine.resolveTurn(
			engine.start(
				initial.copy(
					sides = initial.sides.map { side ->
						if (side.sideId == "side-a") {
							side.copy(participants = side.participants + benched)
						} else {
							side
						}
					},
				),
			),
			listOf(BattleAction.UseSkill("blessing-user", skillId = 849, targetActorId = "blessing-user")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("active-side-healing-cure-affects-active-allies-but-leaves-bench-and-opponents-unchanged")
		assertEquals(65, resolved.participant("blessing-user")?.currentHp)
		assertEquals(100, resolved.participant("blessing-ally")?.currentHp)
		assertEquals(50, resolved.participant("benched-ally")?.currentHp)
		assertEquals(null, resolved.participant("blessing-user")?.majorStatus)
		assertEquals(null, resolved.participant("blessing-ally")?.majorStatus)
		assertEquals(BattleMajorStatus.PARALYSIS, resolved.participant("benched-ally")?.majorStatus)
		assertEquals(BattleMajorStatus.BURN, resolved.participant("opponent-a")?.majorStatus)
		assertEquals(BattleMajorStatus.FREEZE, resolved.participant("opponent-b")?.majorStatus)
		assertEquals(
			listOf("blessing-user", "blessing-ally"),
			resolved.events.filterIsInstance<BattleEvent.StatusCleared>().map { it.actorId },
		)
		assertEquals(
			listOf("blessing-user" to 25, "blessing-ally" to 20),
			resolved.events.filterIsInstance<BattleEvent.SkillHealingApplied>().map { it.actorId to it.amount },
		)
	}

	private fun restSkill() =
		damagingSkill(
			skillId = 156,
			name = "睡觉",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			targetScope = BattleSkillTargetScope.SELF,
			affectedByProtect = false,
			restoresUserBySleeping = true,
		)

	private fun healBellSkill() =
		damagingSkill(
			skillId = 215,
			name = "治愈铃声",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			targetScope = BattleSkillTargetScope.SELF,
			affectedByProtect = false,
			soundBased = true,
			curesUserSideMajorStatuses = true,
		)

	private fun lifeDewSkill() =
		damagingSkill(
			skillId = 791,
			name = "生命水滴",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			targetScope = BattleSkillTargetScope.USER_SIDE_ACTIVE,
			affectedByProtect = false,
			hpEffects = listOf(BattleSkillHpEffect.TargetHealMaxHpFraction(1, 4)),
		)

	private fun lunarBlessingSkill() =
		damagingSkill(
			skillId = 849,
			name = "新月祈祷",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			targetScope = BattleSkillTargetScope.USER_SIDE_ACTIVE,
			affectedByProtect = false,
			curesUserSideActiveMajorStatuses = true,
			hpEffects = listOf(BattleSkillHpEffect.TargetHealMaxHpFraction(1, 4)),
		)
}
