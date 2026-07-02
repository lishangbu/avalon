package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleSkillDynamicPower
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证按双方有效速度差动态计算威力的完整战斗流程。
 *
 * 场景类型：速度差动态威力 场景。
 * 参考来源类型：本地中文资料集对现代技能效果的结构化整理，以及公开成熟对战实现中的公式边界。
 * 验证重点：完整引擎不能只把裸速度传给伤害公式，而必须复用行动排序阶段的有效速度口径；速度能力阶级、麻痹、
 * 天气、场地、道具和一侧速度修正都应先在行动排序组件内收敛，然后由伤害请求传入动态威力公式。
 */
class BattleSpeedRatioPowerSkillTests {
	private val engine = BattleEngine()

	@Test
	fun `electro ball style skill uses staged effective speed in full battle flow`() {
		val scenario = publicBattleRuleScenario(
			name = "electro-ball-style-skill-uses-staged-effective-speed-in-full-battle-flow",
			inputSummary = "使用者速度能力阶级为 -2，实际速度低于目标，使用按使用者/目标有效速度比例决定威力的特殊技能。",
			expectedSummary = "完整流程以行动排序计算出的低速结果选择 40 威力档，最终造成 28 点伤害。",
		)
		val skill = electroBallStyleSkill()
		val state = engine.start(
			initialState(
				first = participant("user", speed = 100, elementId = 13, skill = skill).copy(
					statStages = mapOf(BattleStat.SPEED to -2),
				),
				second = participant("target", speed = 80),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("user", skillId = skill.skillId, targetActorId = "target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		scenario.assertNamed("electro-ball-style-skill-uses-staged-effective-speed-in-full-battle-flow")
		assertEquals(28, resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount)
	}

	@Test
	fun `gyro ball style skill uses staged effective speed in full battle flow`() {
		val scenario = publicBattleRuleScenario(
			name = "gyro-ball-style-skill-uses-staged-effective-speed-in-full-battle-flow",
			inputSummary = "使用者速度能力阶级为 -2，目标裸速度很高，使用按目标/使用者有效速度比例决定威力的物理技能。",
			expectedSummary = "完整流程以行动排序计算出的双方有效速度推导威力并封顶为 150，最终造成 102 点伤害。",
		)
		val skill = gyroBallStyleSkill()
		val state = engine.start(
			initialState(
				first = participant("user", speed = 100, elementId = 9, skill = skill).copy(
					statStages = mapOf(BattleStat.SPEED to -2),
				),
				second = participant("target", speed = 300).copy(maxHp = 200, currentHp = 200),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("user", skillId = skill.skillId, targetActorId = "target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		scenario.assertNamed("gyro-ball-style-skill-uses-staged-effective-speed-in-full-battle-flow")
		assertEquals(102, resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount)
	}

	private fun electroBallStyleSkill(): BattleSkillSlot =
		damagingSkill(
			skillId = 486,
			name = "速度比例威力测试",
			elementId = 13,
			damageClass = BattleDamageClass.SPECIAL,
			power = null,
			dynamicPower = BattleSkillDynamicPower.UserSpeedRatioThresholds(
				thresholds = listOf(
					BattleSkillDynamicPower.SpeedPowerThreshold(minimumRatio = 4, power = 150),
					BattleSkillDynamicPower.SpeedPowerThreshold(minimumRatio = 3, power = 120),
					BattleSkillDynamicPower.SpeedPowerThreshold(minimumRatio = 2, power = 80),
					BattleSkillDynamicPower.SpeedPowerThreshold(minimumRatio = 1, power = 60),
				),
				fallbackPower = 40,
			),
		)

	private fun gyroBallStyleSkill(): BattleSkillSlot =
		damagingSkill(
			skillId = 360,
			name = "反向速度比例威力测试",
			elementId = 9,
			damageClass = BattleDamageClass.PHYSICAL,
			power = null,
			dynamicPower = BattleSkillDynamicPower.TargetToUserSpeedRatio(
				multiplier = 25,
				additivePower = 1,
				maxPower = 150,
			),
		)
}
