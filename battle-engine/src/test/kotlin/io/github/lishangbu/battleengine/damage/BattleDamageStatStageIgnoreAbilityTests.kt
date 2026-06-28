package io.github.lishangbu.battleengine.damage

import io.github.lishangbu.battleengine.damagingSkill
import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.neutralRules
import io.github.lishangbu.battleengine.participant
import io.github.lishangbu.battleengine.publicBattleRuleFixture
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 无视对手伤害公式能力阶级变化特性的公开对照测试。
 *
 * 场景类型：伤害公式 fixture。
 * 参考来源类型：成熟公开对战引擎特性实现和公开伤害公式实现。
 * 验证重点：持有效果的一方作为防守方时，普通伤害公式忽略攻击方攻击或特攻阶级；持有效果的一方作为攻击方时，
 * 普通伤害公式忽略目标防御或特防阶级。该效果不修改双方战斗快照中的真实阶级，只影响当前这次伤害计算。
 */
class BattleDamageStatStageIgnoreAbilityTests {
	private val calculator = BattleDamageCalculator()

	@Test
	fun `defender stat stage ignore ability ignores attacker's physical attack boosts`() {
		val fixture = publicBattleRuleFixture(
			name = "defender-stat-stage-ignore-ability-ignores-attacker-attack-boost",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
				"https://github.com/smogon/damage-calc/blob/master/calc/src/mechanics/gen789.ts",
			),
			inputSummary = "攻击方攻击阶级为 +2，防守方拥有无视对手伤害公式能力阶级变化特性。",
			expectedSummary = "物理伤害按攻击方原始攻击数值计算，不读取 +2 攻击阶级。",
		)

		val result = calculator.calculate(
			BattleDamageRequest(
				attacker = participant("boosted-attacker", speed = 100, elementId = 1).copy(
					statStages = mapOf(BattleStat.ATTACK to 2),
				),
				defender = participant(
					"stage-ignoring-defender",
					speed = 80,
					elementId = 2,
					abilityEffects = listOf(BattleAbilityEffect.IgnoreOpponentDamageStatStages),
				),
				skill = damagingSkill(elementId = 1, damageClass = BattleDamageClass.PHYSICAL, power = 40),
				rules = neutralRules(),
				randomPercent = 100,
			),
		)

		fixture.assertNamed("defender-stat-stage-ignore-ability-ignores-attacker-attack-boost")
		assertEquals(19, result.baseDamage)
		assertEquals(28, result.amount)
	}

	@Test
	fun `attacker stat stage ignore ability ignores defender's physical defense boosts`() {
		val fixture = publicBattleRuleFixture(
			name = "attacker-stat-stage-ignore-ability-ignores-defender-defense-boost",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
				"https://github.com/smogon/damage-calc/blob/master/calc/src/mechanics/gen789.ts",
			),
			inputSummary = "防守方防御阶级为 +2，攻击方拥有无视对手伤害公式能力阶级变化特性。",
			expectedSummary = "物理伤害按防守方原始防御数值计算，不读取 +2 防御阶级。",
		)

		val result = calculator.calculate(
			BattleDamageRequest(
				attacker = participant(
					"stage-ignoring-attacker",
					speed = 100,
					elementId = 1,
					abilityEffects = listOf(BattleAbilityEffect.IgnoreOpponentDamageStatStages),
				),
				defender = participant("boosted-defender", speed = 80, elementId = 2).copy(
					statStages = mapOf(BattleStat.DEFENSE to 2),
				),
				skill = damagingSkill(elementId = 1, damageClass = BattleDamageClass.PHYSICAL, power = 40),
				rules = neutralRules(),
				randomPercent = 100,
			),
		)

		fixture.assertNamed("attacker-stat-stage-ignore-ability-ignores-defender-defense-boost")
		assertEquals(19, result.baseDamage)
		assertEquals(28, result.amount)
	}

	@Test
	fun `stat stage ignore ability applies to special attack and special defense stages`() {
		val fixture = publicBattleRuleFixture(
			name = "stat-stage-ignore-ability-applies-to-special-damage-stages",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
				"https://github.com/smogon/damage-calc/blob/master/calc/src/mechanics/gen789.ts",
			),
			inputSummary = "特殊攻击方特攻阶级为 +2，特殊防守方特防阶级为 +2；双方分别拥有无视对手伤害公式能力阶级变化特性。",
			expectedSummary = "特殊伤害分别在防守方持有和攻击方持有两种情况下按原始特攻或特防数值计算。",
		)

		val defenderIgnoresSpecialAttack = calculator.calculate(
			BattleDamageRequest(
				attacker = participant("boosted-special-attacker", speed = 100, elementId = 1).copy(
					statStages = mapOf(BattleStat.SPECIAL_ATTACK to 2),
				),
				defender = participant(
					"stage-ignoring-special-defender",
					speed = 80,
					elementId = 2,
					abilityEffects = listOf(BattleAbilityEffect.IgnoreOpponentDamageStatStages),
				),
				skill = damagingSkill(elementId = 1, damageClass = BattleDamageClass.SPECIAL, power = 40),
				rules = neutralRules(),
				randomPercent = 100,
			),
		)
		val attackerIgnoresSpecialDefense = calculator.calculate(
			BattleDamageRequest(
				attacker = participant(
					"stage-ignoring-special-attacker",
					speed = 100,
					elementId = 1,
					abilityEffects = listOf(BattleAbilityEffect.IgnoreOpponentDamageStatStages),
				),
				defender = participant("boosted-special-defender", speed = 80, elementId = 2).copy(
					statStages = mapOf(BattleStat.SPECIAL_DEFENSE to 2),
				),
				skill = damagingSkill(elementId = 1, damageClass = BattleDamageClass.SPECIAL, power = 40),
				rules = neutralRules(),
				randomPercent = 100,
			),
		)

		fixture.assertNamed("stat-stage-ignore-ability-applies-to-special-damage-stages")
		assertEquals(19, defenderIgnoresSpecialAttack.baseDamage)
		assertEquals(28, defenderIgnoresSpecialAttack.amount)
		assertEquals(19, attackerIgnoresSpecialDefense.baseDamage)
		assertEquals(28, attackerIgnoresSpecialDefense.amount)
	}
}
