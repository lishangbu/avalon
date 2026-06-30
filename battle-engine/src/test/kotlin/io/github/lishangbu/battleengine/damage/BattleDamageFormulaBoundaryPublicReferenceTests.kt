package io.github.lishangbu.battleengine.damage

import io.github.lishangbu.battleengine.damagingSkill
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleRuleSnapshot
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.model.BattleWeather
import io.github.lishangbu.battleengine.model.ElementEffectivenessChart
import io.github.lishangbu.battleengine.neutralRules
import io.github.lishangbu.battleengine.participant
import io.github.lishangbu.battleengine.publicBattleRuleFixture
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证现代主系列普通伤害公式的边界顺序。
 *
 * 场景类型：纯公式 fixture。
 * 参考来源类型：公开成熟伤害计算实现、公开普通伤害公式说明、公开天气/场地说明和公开道具资料。
 * 这些测试刻意不经过完整回合状态机：命中、要害随机、目标范围和屏障倍率都已经在状态机阶段确定，
 * 公式计算器只负责把“本次伤害请求”稳定折算为 HP 扣减量。
 * 验证重点：基础伤害的整数截断位置、属性一致加成、属性克制连乘、免疫 0 伤害、非免疫最小 1 点伤害、
 * 随机百分比、范围目标倍率、屏障倍率、击中要害、灼伤、攻防阶级、物理/特殊分流、天气、场地和威力阶段道具。
 */
class BattleDamageFormulaBoundaryPublicReferenceTests {
	private val calculator = BattleDamageCalculator()

	@Test
	fun `level fifty neutral same element physical damage keeps base formula`() {
		val fixture = damageFixture(
			name = "level-fifty-neutral-same-element-physical-damage-keeps-base-formula",
			inputSummary = "50 级、40 威力、100 攻击对 100 防御，技能属性和使用者属性一致，随机百分比 100。",
			expectedSummary = "基础伤害为 19，属性一致加成为 1.5，最终伤害为 28。",
		)

		val result = calculate(
			attacker = participant("attacker", speed = 100, elementId = 1),
			defender = participant("defender", speed = 80, elementId = 2),
			skill = damagingSkill(elementId = 1, power = 40),
			randomPercent = 100,
		)

		fixture.assertNamed("level-fifty-neutral-same-element-physical-damage-keeps-base-formula")
		assertEquals(19, result.baseDamage)
		assertEquals(1.5, result.sameElementBonus)
		assertEquals(28, result.amount)
	}

	@Test
	fun `non matching element skips same element bonus`() {
		val fixture = damageFixture(
			name = "non-matching-element-skips-same-element-bonus",
			inputSummary = "使用者属性为 2，技能属性为 1，双方其它公式输入保持中性。",
			expectedSummary = "属性一致加成保持 1.0，最终伤害只使用基础伤害。",
		)

		val result = calculate(
			attacker = participant("attacker", speed = 100, elementId = 2),
			defender = participant("defender", speed = 80, elementId = 3),
			skill = damagingSkill(elementId = 1, power = 40),
			randomPercent = 100,
		)

		fixture.assertNamed("non-matching-element-skips-same-element-bonus")
		assertEquals(1.0, result.sameElementBonus)
		assertEquals(19, result.amount)
	}

	@Test
	fun `dual type effectiveness multiplies every defending element`() {
		val fixture = damageFixture(
			name = "dual-type-effectiveness-multiplies-every-defending-element",
			inputSummary = "攻击属性 1 对目标属性 2 为 2 倍，对目标属性 3 也为 2 倍。",
			expectedSummary = "双属性目标的属性克制按 2 * 2 连乘为 4 倍，再进入最终伤害倍率链。",
		)
		val rules = BattleRuleSnapshot(
			elementChart = ElementEffectivenessChart(mapOf(1L to mapOf(2L to 2.0, 3L to 2.0))),
		)

		val result = calculate(
			attacker = participant("attacker", speed = 100, elementId = 1),
			defender = participant("defender", speed = 80, elementId = 2).copy(elementIds = setOf(2, 3)),
			skill = damagingSkill(elementId = 1, power = 40),
			rules = rules,
			randomPercent = 100,
		)

		fixture.assertNamed("dual-type-effectiveness-multiplies-every-defending-element")
		assertEquals(4.0, result.effectiveness)
		assertEquals(114, result.amount)
	}

	@Test
	fun `type immunity keeps final damage at zero without minimum damage`() {
		val fixture = damageFixture(
			name = "type-immunity-keeps-final-damage-at-zero-without-minimum-damage",
			inputSummary = "攻击属性 1 对目标属性 2 的相性为 0。",
			expectedSummary = "属性免疫把最终伤害固定为 0，不套用非零伤害的最小 1 点规则。",
		)
		val rules = BattleRuleSnapshot(
			elementChart = ElementEffectivenessChart(mapOf(1L to mapOf(2L to 0.0))),
		)

		val result = calculate(
			attacker = participant("attacker", speed = 100, elementId = 1),
			defender = participant("defender", speed = 80, elementId = 2),
			skill = damagingSkill(elementId = 1, power = 40),
			rules = rules,
			randomPercent = 100,
		)

		fixture.assertNamed("type-immunity-keeps-final-damage-at-zero-without-minimum-damage")
		assertEquals(0.0, result.effectiveness)
		assertEquals(0, result.amount)
	}

	@Test
	fun `positive non immune damage is floored to at least one`() {
		val fixture = damageFixture(
			name = "positive-non-immune-damage-is-floored-to-at-least-one",
			inputSummary = "1 威力非本系技能以 85 随机百分比打到双抗目标，属性倍率为 0.25。",
			expectedSummary = "最终倍率链虽然小于 1，但只要不是属性免疫，普通命中仍至少造成 1 点伤害。",
		)
		val rules = BattleRuleSnapshot(
			elementChart = ElementEffectivenessChart(mapOf(1L to mapOf(2L to 0.5, 3L to 0.5))),
		)

		val result = calculate(
			attacker = participant("attacker", speed = 100, elementId = 4),
			defender = participant("defender", speed = 80, elementId = 2).copy(elementIds = setOf(2, 3)),
			skill = damagingSkill(elementId = 1, power = 1),
			rules = rules,
			randomPercent = 85,
		)

		fixture.assertNamed("positive-non-immune-damage-is-floored-to-at-least-one")
		assertEquals(0.25, result.effectiveness)
		assertEquals(1, result.amount)
	}

	@Test
	fun `damage random percent is applied after base damage`() {
		val fixture = damageFixture(
			name = "damage-random-percent-is-applied-after-base-damage",
			inputSummary = "中性非本系 40 威力技能的基础伤害为 19，伤害随机百分比为 85。",
			expectedSummary = "随机百分比参与最终倍率链，19 * 0.85 向下取整为 16。",
		)

		val result = calculate(
			attacker = participant("attacker", speed = 100, elementId = 2),
			defender = participant("defender", speed = 80, elementId = 3),
			skill = damagingSkill(elementId = 1, power = 40),
			randomPercent = 85,
		)

		fixture.assertNamed("damage-random-percent-is-applied-after-base-damage")
		assertEquals(19, result.baseDamage)
		assertEquals(16, result.amount)
	}

	@Test
	fun `spread target modifier participates in final multiplier chain`() {
		val fixture = damageFixture(
			name = "spread-target-modifier-participates-in-final-multiplier-chain",
			inputSummary = "本系中性技能命中多个目标，状态机传入 0.75 目标倍率。",
			expectedSummary = "目标倍率在最终倍率链中与属性一致加成相乘，伤害从 28 降为 21。",
		)

		val result = calculate(
			attacker = participant("attacker", speed = 100, elementId = 1),
			defender = participant("defender", speed = 80, elementId = 2),
			skill = damagingSkill(elementId = 1, power = 40),
			randomPercent = 100,
			targetMultiplier = 0.75,
		)

		fixture.assertNamed("spread-target-modifier-participates-in-final-multiplier-chain")
		assertEquals(0.75, result.targetMultiplier)
		assertEquals(21, result.amount)
	}

	@Test
	fun `side damage reduction multiplier participates in final multiplier chain`() {
		val fixture = damageFixture(
			name = "side-damage-reduction-multiplier-participates-in-final-multiplier-chain",
			inputSummary = "本系中性技能命中处于一侧防守屏障保护下的目标，状态机传入 0.5 屏障倍率。",
			expectedSummary = "屏障倍率在最终倍率链中生效，伤害从 28 降为 14。",
		)

		val result = calculate(
			attacker = participant("attacker", speed = 100, elementId = 1),
			defender = participant("defender", speed = 80, elementId = 2),
			skill = damagingSkill(elementId = 1, power = 40),
			randomPercent = 100,
			sideDamageReductionMultiplier = 0.5,
		)

		fixture.assertNamed("side-damage-reduction-multiplier-participates-in-final-multiplier-chain")
		assertEquals(0.5, result.sideDamageReductionMultiplier)
		assertEquals(14, result.amount)
	}

	@Test
	fun `critical hit applies one and half final damage multiplier`() {
		val fixture = damageFixture(
			name = "critical-hit-applies-one-and-half-final-damage-multiplier",
			inputSummary = "本系中性技能已经由状态机判定为击中要害。",
			expectedSummary = "击中要害倍率按 1.5 进入最终倍率链，伤害从 28 提升到 42。",
		)

		val result = calculate(
			attacker = participant("attacker", speed = 100, elementId = 1),
			defender = participant("defender", speed = 80, elementId = 2),
			skill = damagingSkill(elementId = 1, power = 40),
			randomPercent = 100,
			criticalHit = true,
		)

		fixture.assertNamed("critical-hit-applies-one-and-half-final-damage-multiplier")
		assertEquals(1.5, result.criticalHitMultiplier)
		assertEquals(42, result.amount)
	}

	@Test
	fun `burn halves physical attack before base damage`() {
		val fixture = damageFixture(
			name = "burn-halves-physical-attack-before-base-damage",
			inputSummary = "灼伤使用者以 100 攻击使用本系物理技能。",
			expectedSummary = "灼伤先把物理攻击折半，基础伤害从 19 降到 10，最终伤害为 15。",
		)

		val result = calculate(
			attacker = participant("attacker", speed = 100, elementId = 1).copy(majorStatus = BattleMajorStatus.BURN),
			defender = participant("defender", speed = 80, elementId = 2),
			skill = damagingSkill(elementId = 1, power = 40),
			randomPercent = 100,
		)

		fixture.assertNamed("burn-halves-physical-attack-before-base-damage")
		assertEquals(10, result.baseDamage)
		assertEquals(15, result.amount)
	}

	@Test
	fun `positive attack stage increases physical base damage before final multipliers`() {
		val fixture = damageFixture(
			name = "positive-attack-stage-increases-physical-base-damage-before-final-multipliers",
			inputSummary = "使用者攻击 +2，以本系物理技能攻击中性目标。",
			expectedSummary = "攻击阶级先改变公式攻击值，基础伤害提高到 37，再套用属性一致加成。",
		)

		val result = calculate(
			attacker = participant("attacker", speed = 100, elementId = 1).copy(
				statStages = mapOf(BattleStat.ATTACK to 2),
			),
			defender = participant("defender", speed = 80, elementId = 2),
			skill = damagingSkill(elementId = 1, power = 40),
			randomPercent = 100,
		)

		fixture.assertNamed("positive-attack-stage-increases-physical-base-damage-before-final-multipliers")
		assertEquals(37, result.baseDamage)
		assertEquals(55, result.amount)
	}

	@Test
	fun `positive defense stage reduces physical base damage before final multipliers`() {
		val fixture = damageFixture(
			name = "positive-defense-stage-reduces-physical-base-damage-before-final-multipliers",
			inputSummary = "目标防御 +2，受到本系物理技能攻击。",
			expectedSummary = "防御阶级先改变公式防御值，基础伤害降低到 10，再套用属性一致加成。",
		)

		val result = calculate(
			attacker = participant("attacker", speed = 100, elementId = 1),
			defender = participant("defender", speed = 80, elementId = 2).copy(
				statStages = mapOf(BattleStat.DEFENSE to 2),
			),
			skill = damagingSkill(elementId = 1, power = 40),
			randomPercent = 100,
		)

		fixture.assertNamed("positive-defense-stage-reduces-physical-base-damage-before-final-multipliers")
		assertEquals(10, result.baseDamage)
		assertEquals(15, result.amount)
	}

	@Test
	fun `critical hit ignores unfavorable attack and favorable defense stages`() {
		val fixture = damageFixture(
			name = "critical-hit-ignores-unfavorable-attack-and-favorable-defense-stages-in-formula",
			inputSummary = "使用者攻击 -2，目标防御 +2，本次伤害已判定为击中要害。",
			expectedSummary = "要害公式忽略攻击方不利阶级和防御方有利阶级，基础伤害按 0 阶级计算。",
		)

		val result = calculate(
			attacker = participant("attacker", speed = 100, elementId = 1).copy(
				statStages = mapOf(BattleStat.ATTACK to -2),
			),
			defender = participant("defender", speed = 80, elementId = 2).copy(
				statStages = mapOf(BattleStat.DEFENSE to 2),
			),
			skill = damagingSkill(elementId = 1, power = 40),
			randomPercent = 100,
			criticalHit = true,
		)

		fixture.assertNamed("critical-hit-ignores-unfavorable-attack-and-favorable-defense-stages-in-formula")
		assertEquals(19, result.baseDamage)
		assertEquals(42, result.amount)
	}

	@Test
	fun `critical hit keeps burn physical attack penalty`() {
		val fixture = damageFixture(
			name = "critical-hit-keeps-burn-physical-attack-penalty",
			inputSummary = "灼伤使用者的本系物理技能已判定为击中要害。",
			expectedSummary = "要害不绕过灼伤物理攻击减半，基础伤害仍为 10，然后套用要害倍率。",
		)

		val result = calculate(
			attacker = participant("attacker", speed = 100, elementId = 1).copy(majorStatus = BattleMajorStatus.BURN),
			defender = participant("defender", speed = 80, elementId = 2),
			skill = damagingSkill(elementId = 1, power = 40),
			randomPercent = 100,
			criticalHit = true,
		)

		fixture.assertNamed("critical-hit-keeps-burn-physical-attack-penalty")
		assertEquals(10, result.baseDamage)
		assertEquals(22, result.amount)
	}

	@Test
	fun `special damage uses special attack and special defense stats`() {
		val fixture = damageFixture(
			name = "special-damage-uses-special-attack-and-special-defense-stats",
			inputSummary = "使用者攻击很低但特攻为 200，目标特防为 100，使用 40 威力特殊技能。",
			expectedSummary = "特殊技能只读取特攻/特防，不读取物理攻击/防御，基础伤害按 200 特攻计算。",
		)

		val result = calculate(
			attacker = participant("attacker", speed = 100, elementId = 1).copy(attack = 20, specialAttack = 200),
			defender = participant("defender", speed = 80, elementId = 2).copy(defense = 300, specialDefense = 100),
			skill = damagingSkill(elementId = 1, damageClass = BattleDamageClass.SPECIAL, power = 40),
			randomPercent = 100,
		)

		fixture.assertNamed("special-damage-uses-special-attack-and-special-defense-stats")
		assertEquals(37, result.baseDamage)
		assertEquals(55, result.amount)
	}

	@Test
	fun `sun boosts fire damage in final multiplier chain`() {
		val fixture = damageFixture(
			name = "sun-boosts-fire-damage-in-final-multiplier-chain",
			inputSummary = "晴天下使用火属性本系技能。",
			expectedSummary = "晴天对火属性伤害提供 1.5 倍天气倍率，最终伤害从 28 提升到 42。",
		)

		val result = calculate(
			attacker = participant("attacker", speed = 100, elementId = 10),
			defender = participant("defender", speed = 80, elementId = 2),
			skill = damagingSkill(elementId = 10, power = 40),
			environment = BattleEnvironment(weather = BattleWeather.SUN),
			randomPercent = 100,
		)

		fixture.assertNamed("sun-boosts-fire-damage-in-final-multiplier-chain")
		assertEquals(1.5, result.weatherMultiplier)
		assertEquals(42, result.amount)
	}

	@Test
	fun `rain weakens fire damage in final multiplier chain`() {
		val fixture = damageFixture(
			name = "rain-weakens-fire-damage-in-final-multiplier-chain",
			inputSummary = "下雨时使用火属性本系技能。",
			expectedSummary = "雨天对火属性伤害提供 0.5 倍天气倍率，最终伤害从 28 降到 14。",
		)

		val result = calculate(
			attacker = participant("attacker", speed = 100, elementId = 10),
			defender = participant("defender", speed = 80, elementId = 2),
			skill = damagingSkill(elementId = 10, power = 40),
			environment = BattleEnvironment(weather = BattleWeather.RAIN),
			randomPercent = 100,
		)

		fixture.assertNamed("rain-weakens-fire-damage-in-final-multiplier-chain")
		assertEquals(0.5, result.weatherMultiplier)
		assertEquals(14, result.amount)
	}

	@Test
	fun `grassy terrain boosts grounded grass damage`() {
		val fixture = damageFixture(
			name = "grassy-terrain-boosts-grounded-grass-damage-in-formula",
			inputSummary = "青草场地中，接地使用者使用草属性本系技能。",
			expectedSummary = "场地倍率为 1.3，最终伤害从 28 提升到 37。",
		)

		val result = calculate(
			attacker = participant("attacker", speed = 100, elementId = 12, grounded = true),
			defender = participant("defender", speed = 80, elementId = 2),
			skill = damagingSkill(elementId = 12, power = 40),
			environment = BattleEnvironment(terrain = BattleTerrain.GRASSY),
			randomPercent = 100,
		)

		fixture.assertNamed("grassy-terrain-boosts-grounded-grass-damage-in-formula")
		assertEquals(1.3, result.terrainMultiplier)
		assertEquals(37, result.amount)
	}

	@Test
	fun `grassy terrain weakens tagged ground move against grounded target`() {
		val fixture = damageFixture(
			name = "grassy-terrain-weakens-tagged-ground-move-against-grounded-target",
			inputSummary = "青草场地中，带震动削弱标签的地面属性技能命中接地目标。",
			expectedSummary = "场地倍率为 0.5，最终伤害从 28 降到 14。",
		)

		val result = calculate(
			attacker = participant("attacker", speed = 100, elementId = 5),
			defender = participant("defender", speed = 80, elementId = 2, grounded = true),
			skill = damagingSkill(elementId = 5, power = 40, weakenedByGrassyTerrain = true),
			environment = BattleEnvironment(terrain = BattleTerrain.GRASSY),
			randomPercent = 100,
		)

		fixture.assertNamed("grassy-terrain-weakens-tagged-ground-move-against-grounded-target")
		assertEquals(0.5, result.terrainMultiplier)
		assertEquals(14, result.amount)
	}

	@Test
	fun `power stage item changes base damage before final multipliers`() {
		val fixture = damageFixture(
			name = "power-stage-item-changes-base-damage-before-final-multipliers",
			inputSummary = "使用者携带物理威力强化道具，40 威力本系物理技能获得 1.1 倍威力阶段倍率。",
			expectedSummary = "威力先向下取整为 44，再进入基础伤害公式，基础伤害从 19 提升到 21。",
		)

		val result = calculate(
			attacker = participant(
				"attacker",
				speed = 100,
				elementId = 1,
				itemEffects = listOf(
					BattleItemEffect.DamageClassPowerBoost(
						damageClasses = setOf(BattleDamageClass.PHYSICAL),
						multiplier = 1.1,
					),
				),
			),
			defender = participant("defender", speed = 80, elementId = 2),
			skill = damagingSkill(elementId = 1, damageClass = BattleDamageClass.PHYSICAL, power = 40),
			randomPercent = 100,
		)

		fixture.assertNamed("power-stage-item-changes-base-damage-before-final-multipliers")
		assertEquals(21, result.baseDamage)
		assertEquals(31, result.amount)
	}

	private fun damageFixture(
		name: String,
		inputSummary: String,
		expectedSummary: String,
	) = publicBattleRuleFixture(
		name = name,
		inputSummary = inputSummary,
		expectedSummary = expectedSummary,
	)

	private fun calculate(
		attacker: io.github.lishangbu.battleengine.model.BattleParticipant,
		defender: io.github.lishangbu.battleengine.model.BattleParticipant,
		skill: io.github.lishangbu.battleengine.model.BattleSkillSlot,
		rules: BattleRuleSnapshot = neutralRules(),
		environment: BattleEnvironment = BattleEnvironment(),
		randomPercent: Int,
		targetMultiplier: Double = 1.0,
		sideDamageReductionMultiplier: Double = 1.0,
		criticalHit: Boolean = false,
	): BattleDamageResult =
		calculator.calculate(
			BattleDamageRequest(
				attacker = attacker,
				defender = defender,
				skill = skill,
				rules = rules,
				environment = environment,
				randomPercent = randomPercent,
				targetMultiplier = targetMultiplier,
				sideDamageReductionMultiplier = sideDamageReductionMultiplier,
				criticalHit = criticalHit,
			),
		)
}
