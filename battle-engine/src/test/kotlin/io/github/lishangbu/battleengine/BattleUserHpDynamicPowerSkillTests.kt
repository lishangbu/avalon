package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleSkillDynamicPower
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证按使用者当前 HP 比例分档的动态威力技能族。
 *
 * 场景类型：普通伤害公式前动态基础威力 场景。
 * 参考来源类型：公开成熟模拟器实现和公开规则说明。现代规则使用
 * `floor(48 * 使用者当前 HP / 使用者最大 HP)` 得到离散 X 值，再按 X 的区间选择 20/40/80/100/150/200 威力。
 * 测试故意使用不触发属性一致加成的技能属性，让断言只反映动态威力分档和普通伤害公式，不混入属性一致加成倍率。
 */
class BattleUserHpDynamicPowerSkillTests {
	private val engine = BattleEngine()

	@Test
	fun `user hp ratio dynamic power uses modern six step thresholds`() {
		val scenario = publicBattleRuleScenario(
			name = "user-hp-ratio-dynamic-power-uses-modern-six-step-thresholds",
			inputSummary = "同一普通物理伤害技能在使用者不同当前 HP 下结算，技能基础威力来自 48 倍 HP 比例分档。",
			expectedSummary = "高 HP 使用 20 威力，HP 越低依次进入 40/80/100/150/200 威力档，1 HP 与 4 HP 都达到最高档。",
		)
		val cases = listOf(
			HpPowerCase(currentHp = 100, expectedDamage = 10),
			HpPowerCase(currentHp = 68, expectedDamage = 19),
			HpPowerCase(currentHp = 35, expectedDamage = 37),
			HpPowerCase(currentHp = 20, expectedDamage = 46),
			HpPowerCase(currentHp = 10, expectedDamage = 68),
			HpPowerCase(currentHp = 4, expectedDamage = 90),
			HpPowerCase(currentHp = 1, expectedDamage = 90),
		)

		val actualDamageByHp = cases.associate { case ->
			val state = engine.start(
				initialState(
					first = participant(
						"attacker-${case.currentHp}",
						speed = 100,
						currentHp = case.currentHp,
						elementId = 2,
						skill = userHpPowerSkill(),
					),
					second = participant("target-${case.currentHp}", speed = 50, elementId = 3),
				),
			)
			val resolved = engine.resolveTurn(
				state,
				listOf(
					BattleAction.UseSkill(
						"attacker-${case.currentHp}",
						skillId = 9175,
						targetActorId = "target-${case.currentHp}",
					),
				),
				ScriptedBattleRandom(listOf(1, 15)),
			)
			case.currentHp to resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount
		}

		scenario.assertNamed("user-hp-ratio-dynamic-power-uses-modern-six-step-thresholds")
		assertEquals(cases.associate { it.currentHp to it.expectedDamage }, actualDamageByHp)
	}

	private fun userHpPowerSkill() =
		damagingSkill(
			skillId = 9175,
			name = "当前体力威力测试",
			elementId = 1,
			power = null,
			dynamicPower = BattleSkillDynamicPower.UserHpFractionThresholds(
				scale = 48,
				thresholds = listOf(
					BattleSkillDynamicPower.HpPowerThreshold(maxScaledHpInclusive = 1, power = 200),
					BattleSkillDynamicPower.HpPowerThreshold(maxScaledHpInclusive = 4, power = 150),
					BattleSkillDynamicPower.HpPowerThreshold(maxScaledHpInclusive = 9, power = 100),
					BattleSkillDynamicPower.HpPowerThreshold(maxScaledHpInclusive = 16, power = 80),
					BattleSkillDynamicPower.HpPowerThreshold(maxScaledHpInclusive = 32, power = 40),
				),
				fallbackPower = 20,
			),
		)

	private data class HpPowerCase(
		val currentHp: Int,
		val expectedDamage: Int,
	)
}
