package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 验证技能成功命中后的强制替换效果。
 *
 * 场景类型：技能效果 场景。
 * 参考来源类型：公开成熟模拟器实现和公开规则说明。现代规则中，部分变化技能会在命中后直接强制目标侧后备上场，
 * 部分伤害技能会先造成伤害，再触发同样的强制替换流程。
 * 验证重点：强制替换发生在命中、保护、替身和伤害流程之后；合法后备多于 1 个时消费随机数决定上场成员；
 * 换入事件继续复用普通替换事件和后续入场规则。
 */
class BattleForcedSwitchSkillTests {
	private val engine = BattleEngine()

	@Test
	fun `status skill forces target side to switch to a random bench participant`() {
		val scenario = publicBattleRuleScenario(
			name = "status-skill-forces-target-side-random-bench-switch",
			inputSummary = "变化类强制替换技能命中目标，目标侧有两个可战斗后备成员，随机脚本选择第二个后备。",
			expectedSummary = "目标离场并清理离场运行态，第二个后备上场；事件流先记录强制替换选择，再记录席位替换。",
		)
		val forceSwitchSkill = damagingSkill(
			skillId = 46,
			name = "吼叫",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			forceTargetSwitch = true,
			priority = -6,
		)
		val random = ScriptedBattleRandom(listOf(1))
		val state = engine.start(
			initialState(
				first = participant("actor", speed = 100, skill = forceSwitchSkill),
				second = participant("target", speed = 80),
				secondBench = listOf(
					participant("reserve-a", speed = 60),
					participant("reserve-b", speed = 70),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("actor", skillId = 46, targetActorId = "target")),
			random,
		)

		scenario.assertNamed("status-skill-forces-target-side-random-bench-switch")
		assertEquals(listOf("reserve-b"), resolved.sides.single { it.sideId == "side-b" }.activeActorIds)
		assertEquals(listOf("forced switch target for 46"), random.consumedReasons())
		val forcedSwitch = resolved.events.filterIsInstance<BattleEvent.TargetForcedSwitchSelected>().single()
		assertEquals("target", forcedSwitch.targetActorId)
		assertEquals("reserve-b", forcedSwitch.nextActorId)
		val switched = resolved.events.filterIsInstance<BattleEvent.ParticipantSwitched>().single()
		assertTrue(switched.forced)
		assertEquals("target", switched.previousActorId)
		assertEquals("reserve-b", switched.nextActorId)
	}

	@Test
	fun `damaging skill applies damage before forcing target switch`() {
		val scenario = publicBattleRuleScenario(
			name = "damaging-skill-applies-damage-before-forced-target-switch",
			inputSummary = "物理伤害类强制替换技能命中目标，目标侧有一个可战斗后备成员。",
			expectedSummary = "目标先承受技能伤害，随后被强制换下；只有一个合法后备时不额外消费随机数。",
		)
		val forceSwitchSkill = damagingSkill(
			skillId = 525,
			name = "龙尾",
			power = 60,
			accuracy = null,
			forceTargetSwitch = true,
			priority = -6,
		)
		val random = ScriptedBattleRandom(listOf(1, 15))
		val state = engine.start(
			initialState(
				first = participant("actor", speed = 100, skill = forceSwitchSkill),
				second = participant("target", speed = 80, currentHp = 100),
				secondBench = listOf(participant("reserve", speed = 60)),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("actor", skillId = 525, targetActorId = "target")),
			random,
		)

		scenario.assertNamed("damaging-skill-applies-damage-before-forced-target-switch")
		assertEquals(listOf("reserve"), resolved.sides.single { it.sideId == "side-b" }.activeActorIds)
		assertEquals(listOf("critical hit for 525", "damage random for 525"), random.consumedReasons())
		assertTrue((resolved.participant("target")?.currentHp ?: 100) < 100)
		val damageIndex = resolved.events.indexOfFirst { it is BattleEvent.DamageApplied }
		val forcedSwitchIndex = resolved.events.indexOfFirst { it is BattleEvent.TargetForcedSwitchSelected }
		assertTrue(damageIndex >= 0)
		assertTrue(forcedSwitchIndex > damageIndex)
	}
}
