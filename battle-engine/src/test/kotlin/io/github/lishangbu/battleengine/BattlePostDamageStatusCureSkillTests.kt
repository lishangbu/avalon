package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleSkillPostDamageStatusCure
import io.github.lishangbu.battleengine.model.BattleSkillPowerMultiplier
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证伤害后治愈目标主要异常的技能规则。
 *
 * 场景类型：技能命中后状态治愈 场景。
 * 参考来源类型：本地中文资料集对现代技能效果的结构化整理，以及公开成熟对战实现中的事件顺序。
 * 验证重点：清醒、唤醒巴掌这类技能先按目标状态修正威力并造成实际伤害，再治愈对应主要异常；泡影的咏叹调
 * 只在目标本体实际受伤后治愈灼伤。替身承受伤害时，目标本体没有实际损失 HP，因此不会清除目标主要异常。
 */
class BattlePostDamageStatusCureSkillTests {
	private val engine = BattleEngine()

	@Test
	fun `paralysis cure damage skill doubles power then cures target paralysis`() {
		val scenario = publicBattleRuleScenario(
			name = "paralysis-cure-damage-skill-doubles-power-then-cures-target-paralysis",
			inputSummary = "目标处于麻痹，使用者用对麻痹目标威力翻倍且伤害后治愈麻痹的物理技能命中目标。",
			expectedSummary = "技能以双倍威力造成 94 点实际伤害，随后清除目标麻痹并记录状态解除事件。",
		)
		val skill = statusCureDamageSkill(
			status = BattleMajorStatus.PARALYSIS,
			powerMultiplier = BattleSkillPowerMultiplier.TargetMajorStatus(
				statuses = setOf(BattleMajorStatus.PARALYSIS),
				multiplier = 2.0,
			),
		)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = skill),
				second = participant("target", speed = 50).copy(majorStatus = BattleMajorStatus.PARALYSIS),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		scenario.assertNamed("paralysis-cure-damage-skill-doubles-power-then-cures-target-paralysis")
		assertEquals(94, resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount)
		assertEquals(null, resolved.participant("target")?.majorStatus)
		val cleared = resolved.events.filterIsInstance<BattleEvent.StatusCleared>().single()
		assertEquals("target", cleared.actorId)
		assertEquals(BattleMajorStatus.PARALYSIS, cleared.status)
	}

	@Test
	fun `sleep cure damage skill doubles power then wakes target`() {
		val scenario = publicBattleRuleScenario(
			name = "sleep-cure-damage-skill-doubles-power-then-wakes-target",
			inputSummary = "目标处于睡眠，使用者用对睡眠目标威力翻倍且伤害后唤醒目标的物理技能命中目标。",
			expectedSummary = "技能以双倍威力造成实际伤害，随后清除目标睡眠和睡眠行动计数。",
		)
		val skill = statusCureDamageSkill(
			status = BattleMajorStatus.SLEEP,
			powerMultiplier = BattleSkillPowerMultiplier.TargetMajorStatus(
				statuses = setOf(BattleMajorStatus.SLEEP),
				multiplier = 2.0,
			),
		)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = skill),
				second = participant("target", speed = 50).copy(
					majorStatus = BattleMajorStatus.SLEEP,
					sleepTurnsRemaining = 2,
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		scenario.assertNamed("sleep-cure-damage-skill-doubles-power-then-wakes-target")
		assertEquals(94, resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount)
		assertEquals(null, resolved.participant("target")?.majorStatus)
		assertEquals(0, resolved.participant("target")?.sleepTurnsRemaining)
		val cleared = resolved.events.filterIsInstance<BattleEvent.StatusCleared>().single()
		assertEquals(BattleMajorStatus.SLEEP, cleared.status)
	}

	@Test
	fun `burn cure damage skill cures target burn after actual damage`() {
		val scenario = publicBattleRuleScenario(
			name = "burn-cure-damage-skill-cures-target-burn-after-actual-damage",
			inputSummary = "目标处于灼伤，使用者用伤害后治愈目标灼伤的特殊技能命中目标本体。",
			expectedSummary = "目标实际损失 HP 后，灼伤被治愈并产生状态解除事件。",
		)
		val skill = damagingSkill(
			name = "灼伤治愈伤害测试",
			power = 90,
			postDamageStatusCures = listOf(
				BattleSkillPostDamageStatusCure(statuses = setOf(BattleMajorStatus.BURN)),
			),
		)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = skill),
				second = participant("target", speed = 50).copy(majorStatus = BattleMajorStatus.BURN),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		scenario.assertNamed("burn-cure-damage-skill-cures-target-burn-after-actual-damage")
		assertEquals(61, resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount)
		assertEquals(null, resolved.participant("target")?.majorStatus)
		val cleared = resolved.events.filterIsInstance<BattleEvent.StatusCleared>().single()
		assertEquals(BattleMajorStatus.BURN, cleared.status)
	}

	@Test
	fun `post damage status cure does not trigger when substitute takes damage`() {
		val scenario = publicBattleRuleScenario(
			name = "post-damage-status-cure-does-not-trigger-when-substitute-takes-damage",
			inputSummary = "目标处于灼伤且拥有替身，使用者用伤害后治愈目标灼伤的技能命中。",
			expectedSummary = "伤害由替身承受，目标本体没有实际损失 HP，因此灼伤不会被治愈。",
		)
		val skill = damagingSkill(
			name = "替身阻止治愈测试",
			power = 90,
			postDamageStatusCures = listOf(
				BattleSkillPostDamageStatusCure(statuses = setOf(BattleMajorStatus.BURN)),
			),
		)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = skill),
				second = participant("target", speed = 50).copy(
					majorStatus = BattleMajorStatus.BURN,
					substituteHp = 25,
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		scenario.assertNamed("post-damage-status-cure-does-not-trigger-when-substitute-takes-damage")
		assertEquals(BattleMajorStatus.BURN, resolved.participant("target")?.majorStatus)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.StatusCleared>())
	}

	private fun statusCureDamageSkill(
		status: BattleMajorStatus,
		powerMultiplier: BattleSkillPowerMultiplier,
	) = damagingSkill(
		name = "状态治愈伤害测试",
		power = 70,
		conditionalPowerMultipliers = listOf(powerMultiplier),
		postDamageStatusCures = listOf(BattleSkillPostDamageStatusCure(statuses = setOf(status))),
	)
}
