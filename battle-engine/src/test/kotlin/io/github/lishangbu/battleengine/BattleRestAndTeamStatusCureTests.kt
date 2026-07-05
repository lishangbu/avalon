package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleMajorStatus
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
}
