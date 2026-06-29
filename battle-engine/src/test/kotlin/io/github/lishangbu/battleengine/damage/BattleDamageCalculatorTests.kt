package io.github.lishangbu.battleengine.damage

import io.github.lishangbu.battleengine.damagingSkill
import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleRuleSnapshot
import io.github.lishangbu.battleengine.model.BattleMajorStatus
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
 * 验证第一版普通伤害公式。
 *
 * 场景类型：公式级 fixture。
 * 参考来源类型：公开主系列普通伤害公式的通用结构；本测试覆盖可独立确认的等级、威力、攻防、随机、
 * 属性一致加成、属性克制、击中要害、天气、状态、道具和特性。
 * 验证重点：基础伤害中间值稳定、随机百分比可控、0 倍免疫不产生最小 1 点伤害。
 */
class BattleDamageCalculatorTests {
	private val calculator = BattleDamageCalculator()

	@Test
	fun `standard damage applies same element bonus and effectiveness`() {
		val result = calculator.calculate(
			BattleDamageRequest(
				attacker = participant("attacker", speed = 100, elementId = 1),
				defender = participant("defender", speed = 80, elementId = 2),
				skill = damagingSkill(elementId = 1, power = 40),
				rules = BattleRuleSnapshot(
					elementChart = ElementEffectivenessChart(
						mapOf(1L to mapOf(2L to 2.0)),
					),
				),
				randomPercent = 100,
			),
		)

		assertEquals(19, result.baseDamage)
		assertEquals(1.5, result.sameElementBonus)
		assertEquals(2.0, result.effectiveness)
		assertEquals(57, result.amount)
	}

	@Test
	fun `immunity effectiveness produces zero damage`() {
		val result = calculator.calculate(
			BattleDamageRequest(
				attacker = participant("attacker", speed = 100, elementId = 1),
				defender = participant("defender", speed = 80, elementId = 2),
				skill = damagingSkill(elementId = 1, power = 40),
				rules = BattleRuleSnapshot(
					elementChart = ElementEffectivenessChart(
						mapOf(1L to mapOf(2L to 0.0)),
					),
				),
				randomPercent = 100,
			),
		)

		assertEquals(0.0, result.effectiveness)
		assertEquals(0, result.amount)
	}

	@Test
	fun `neutral non matching element damage keeps minimum integer formula`() {
		val result = calculator.calculate(
			BattleDamageRequest(
				attacker = participant("attacker", speed = 100, elementId = 2),
				defender = participant("defender", speed = 80, elementId = 3),
				skill = damagingSkill(elementId = 1, power = 40),
				rules = neutralRules(),
				randomPercent = 100,
			),
		)

		assertEquals(19, result.baseDamage)
		assertEquals(1.0, result.sameElementBonus)
		assertEquals(1.0, result.effectiveness)
		assertEquals(19, result.amount)
	}

	@Test
	fun `multi target damage modifier reduces spread damage`() {
		val result = calculator.calculate(
			BattleDamageRequest(
				attacker = participant("attacker", speed = 100, elementId = 1),
				defender = participant("defender", speed = 80, elementId = 2),
				skill = damagingSkill(elementId = 1, power = 40),
				rules = neutralRules(),
				randomPercent = 100,
				targetMultiplier = 0.75,
			),
		)

		assertEquals(19, result.baseDamage)
		assertEquals(0.75, result.targetMultiplier)
		assertEquals(21, result.amount)
	}

	@Test
	fun `burn halves physical attacking stat before damage`() {
		val fixture = publicBattleRuleFixture(
			name = "burn-halves-physical-attacking-stat-before-damage",
			sourceUrls = listOf(
				"https://bulbapedia.bulbagarden.net/wiki/Burn_(status_condition)",
				"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
			),
			inputSummary = "灼伤状态的使用者以 100 攻击使用物理技能攻击无特殊防御修正目标。",
			expectedSummary = "物理攻击数值先按灼伤减半参与普通伤害公式，最终伤害低于同条件未灼伤物理攻击。",
		)
		val result = calculator.calculate(
			BattleDamageRequest(
				attacker = participant("attacker", speed = 100, elementId = 1).copy(majorStatus = BattleMajorStatus.BURN),
				defender = participant("defender", speed = 80, elementId = 2),
				skill = damagingSkill(elementId = 1, power = 40),
				rules = neutralRules(),
				randomPercent = 100,
			),
		)

		fixture.assertNamed("burn-halves-physical-attacking-stat-before-damage")
		assertEquals(10, result.baseDamage)
		assertEquals(15, result.amount)
	}

	@Test
	fun `attack stage modifies physical damage`() {
		val result = calculator.calculate(
			BattleDamageRequest(
				attacker = participant("attacker", speed = 100, elementId = 1).copy(
					statStages = mapOf(BattleStat.ATTACK to 2),
				),
				defender = participant("defender", speed = 80, elementId = 2),
				skill = damagingSkill(elementId = 1, power = 40),
				rules = neutralRules(),
				randomPercent = 100,
			),
		)

		assertEquals(37, result.baseDamage)
		assertEquals(55, result.amount)
	}

	@Test
	fun `critical hit multiplies damage and ignores unfavorable attack and favorable defense stages`() {
		val result = calculator.calculate(
			BattleDamageRequest(
				attacker = participant("attacker", speed = 100, elementId = 1).copy(
					statStages = mapOf(BattleStat.ATTACK to -2),
				),
				defender = participant("defender", speed = 80, elementId = 2).copy(
					statStages = mapOf(BattleStat.DEFENSE to 2),
				),
				skill = damagingSkill(elementId = 1, power = 40),
				rules = neutralRules(),
				randomPercent = 100,
				criticalHit = true,
			),
		)

		assertEquals(19, result.baseDamage)
		assertEquals(1.5, result.criticalHitMultiplier)
		assertEquals(42, result.amount)
	}

	@Test
	fun `critical hit still keeps burn physical penalty`() {
		val result = calculator.calculate(
			BattleDamageRequest(
				attacker = participant("attacker", speed = 100, elementId = 1).copy(majorStatus = BattleMajorStatus.BURN),
				defender = participant("defender", speed = 80, elementId = 2),
				skill = damagingSkill(elementId = 1, power = 40),
				rules = neutralRules(),
				randomPercent = 100,
				criticalHit = true,
			),
		)

		assertEquals(10, result.baseDamage)
		assertEquals(1.5, result.criticalHitMultiplier)
		assertEquals(22, result.amount)
	}

	@Test
	fun `side damage reduction multiplier reduces standard damage`() {
		val result = calculator.calculate(
			BattleDamageRequest(
				attacker = participant("attacker", speed = 100, elementId = 1),
				defender = participant("defender", speed = 80, elementId = 2),
				skill = damagingSkill(elementId = 1, power = 40),
				rules = neutralRules(),
				randomPercent = 100,
				sideDamageReductionMultiplier = 0.5,
			),
		)

		assertEquals(0.5, result.sideDamageReductionMultiplier)
		assertEquals(14, result.amount)
	}

	@Test
	fun `low hp ability and damage boost item multiply damage`() {
		val result = calculator.calculate(
			BattleDamageRequest(
				attacker = participant(
					"attacker",
					speed = 100,
					currentHp = 30,
					elementId = 1,
					abilityEffects = listOf(BattleAbilityEffect.LowHpElementDamageBoost(elementId = 1)),
					itemEffects = listOf(BattleItemEffect.DamageBoostWithRecoil(multiplier = 1.3, recoilDenominator = 10)),
				),
				defender = participant("defender", speed = 80, elementId = 2),
				skill = damagingSkill(elementId = 1, power = 40),
				rules = neutralRules(),
				randomPercent = 100,
			),
		)

		assertEquals(1.5, result.abilityMultiplier)
		assertEquals(1.3, result.itemMultiplier)
		assertEquals(55, result.amount)
	}

	@Test
	fun `low hp element ability boosts matching element damage at one third hp`() {
		val fixture = publicBattleRuleFixture(
			name = "low-hp-element-ability-boosts-matching-damage-at-threshold",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
				"https://bulbapedia.bulbagarden.net/wiki/Swarm_(Ability)",
			),
			inputSummary = "使用者当前 HP 等于最大 HP 的 1/3，拥有低体力强化虫属性伤害的结构化特性，并使用虫属性技能。",
			expectedSummary = "技能属性匹配且 HP 达到阈值时，伤害倍率按 1.5 叠乘；高于阈值或属性不匹配时不触发该倍率。",
		)

		val boosted = calculator.calculate(
			BattleDamageRequest(
				attacker = participant(
					"bug-attacker",
					speed = 100,
					currentHp = 33,
					elementId = 7,
					abilityEffects = listOf(BattleAbilityEffect.LowHpElementDamageBoost(elementId = 7)),
				),
				defender = participant("defender", speed = 80, elementId = 1),
				skill = damagingSkill(elementId = 7, power = 40),
				rules = neutralRules(),
				randomPercent = 100,
			),
		)
		val aboveThreshold = calculator.calculate(
			BattleDamageRequest(
				attacker = participant(
					"bug-attacker",
					speed = 100,
					currentHp = 34,
					elementId = 7,
					abilityEffects = listOf(BattleAbilityEffect.LowHpElementDamageBoost(elementId = 7)),
				),
				defender = participant("defender", speed = 80, elementId = 1),
				skill = damagingSkill(elementId = 7, power = 40),
				rules = neutralRules(),
				randomPercent = 100,
			),
		)
		val nonMatching = calculator.calculate(
			BattleDamageRequest(
				attacker = participant(
					"bug-attacker",
					speed = 100,
					currentHp = 33,
					elementId = 7,
					abilityEffects = listOf(BattleAbilityEffect.LowHpElementDamageBoost(elementId = 7)),
				),
				defender = participant("defender", speed = 80, elementId = 1),
				skill = damagingSkill(elementId = 1, power = 40),
				rules = neutralRules(),
				randomPercent = 100,
			),
		)

		fixture.assertNamed("low-hp-element-ability-boosts-matching-damage-at-threshold")
		assertEquals(1.5, boosted.abilityMultiplier)
		assertEquals(42, boosted.amount)
		assertEquals(1.0, aboveThreshold.abilityMultiplier)
		assertEquals(28, aboveThreshold.amount)
		assertEquals(1.0, nonMatching.abilityMultiplier)
		assertEquals(19, nonMatching.amount)
	}

	@Test
	fun `weather element ability boosts matching effective element damage only in matching weather`() {
		val fixture = publicBattleRuleFixture(
			name = "weather-element-ability-boosts-matching-element-in-sandstorm",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
				"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
			),
			inputSummary = "使用者拥有沙暴下强化岩石、地面和钢属性技能的结构化特性，在沙暴中分别使用匹配属性、非匹配属性和天气改属性技能。",
			expectedSummary = "沙暴存在且本次有效属性匹配时获得 1.3 倍特性倍率；天气不匹配或有效属性不匹配时不触发。",
		)
		val attacker = participant(
			"weather-attacker",
			speed = 100,
			elementId = 1,
			abilityEffects = listOf(
				BattleAbilityEffect.WeatherElementDamageBoost(
					weather = BattleWeather.SANDSTORM,
					elementIds = setOf(5, 6, 9),
				),
			),
		)
		val sandstorm = BattleEnvironment(weather = BattleWeather.SANDSTORM)

		val matching = calculator.calculate(
			BattleDamageRequest(
				attacker = attacker,
				defender = participant("defender", speed = 80, elementId = 2),
				skill = damagingSkill(elementId = 6, power = 40),
				rules = neutralRules(),
				environment = sandstorm,
				randomPercent = 100,
			),
		)
		val noWeather = calculator.calculate(
			BattleDamageRequest(
				attacker = attacker,
				defender = participant("defender", speed = 80, elementId = 2),
				skill = damagingSkill(elementId = 6, power = 40),
				rules = neutralRules(),
				randomPercent = 100,
			),
		)
		val nonMatchingElement = calculator.calculate(
			BattleDamageRequest(
				attacker = attacker,
				defender = participant("defender", speed = 80, elementId = 2),
				skill = damagingSkill(elementId = 11, power = 40),
				rules = neutralRules(),
				environment = sandstorm,
				randomPercent = 100,
			),
		)
		val weatherOverride = calculator.calculate(
			BattleDamageRequest(
				attacker = attacker,
				defender = participant("defender", speed = 80, elementId = 2),
				skill = damagingSkill(
					elementId = 1,
					power = 40,
					elementOverridesByWeather = mapOf(BattleWeather.SANDSTORM to 6),
				),
				rules = neutralRules(),
				environment = sandstorm,
				randomPercent = 100,
			),
		)

		fixture.assertNamed("weather-element-ability-boosts-matching-element-in-sandstorm")
		assertEquals(1.3, matching.abilityMultiplier)
		assertEquals(24, matching.amount)
		assertEquals(1.0, noWeather.abilityMultiplier)
		assertEquals(19, noWeather.amount)
		assertEquals(1.0, nonMatchingElement.abilityMultiplier)
		assertEquals(19, nonMatchingElement.amount)
		assertEquals(1.3, weatherOverride.abilityMultiplier)
		assertEquals(24, weatherOverride.amount)
	}

	@Test
	fun `punch based ability boosts only tagged skill damage`() {
		val fixture = publicBattleRuleFixture(
			name = "punch-based-ability-boosts-punch-tagged-skill-damage",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
				"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
			),
			inputSummary = "使用者拥有拳类技能伤害提升特性，分别使用带拳类标签和不带拳类标签的一般属性 80 威力物理技能。",
			expectedSummary = "带拳类标签的技能在最终伤害中获得 1.2 倍特性倍率，不带标签的同威力技能保持原伤害。",
		)
		val attacker = participant(
			"attacker",
			speed = 100,
			elementId = 1,
			abilityEffects = listOf(BattleAbilityEffect.PunchBasedSkillDamageBoost()),
		)

		val tagged = calculator.calculate(
			BattleDamageRequest(
				attacker = attacker,
				defender = participant("defender", speed = 80, elementId = 2),
				skill = damagingSkill(skillId = 5, name = "百万吨重拳", elementId = 1, power = 80, punchBased = true),
				rules = neutralRules(),
				randomPercent = 100,
			),
		)
		val untagged = calculator.calculate(
			BattleDamageRequest(
				attacker = attacker,
				defender = participant("defender", speed = 80, elementId = 2),
				skill = damagingSkill(skillId = 1, name = "普通拳名技能", elementId = 1, power = 80, punchBased = false),
				rules = neutralRules(),
				randomPercent = 100,
			),
		)

		fixture.assertNamed("punch-based-ability-boosts-punch-tagged-skill-damage")
		assertEquals(1.2, tagged.abilityMultiplier)
		assertEquals(66, tagged.amount)
		assertEquals(1.0, untagged.abilityMultiplier)
		assertEquals(55, untagged.amount)
	}

	@Test
	fun `slicing based ability boosts only tagged skill damage`() {
		val fixture = publicBattleRuleFixture(
			name = "slicing-based-ability-boosts-slicing-tagged-skill-damage",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
				"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
			),
			inputSummary = "使用者拥有切割类技能伤害提升特性，分别使用带切割标签和不带切割标签的一般属性 70 威力物理技能。",
			expectedSummary = "带切割标签的技能在最终伤害中获得 1.5 倍特性倍率，不带标签的同威力技能保持原伤害。",
		)
		val attacker = participant(
			"attacker",
			speed = 100,
			elementId = 1,
			abilityEffects = listOf(BattleAbilityEffect.SlicingBasedSkillDamageBoost()),
		)

		val tagged = calculator.calculate(
			BattleDamageRequest(
				attacker = attacker,
				defender = participant("defender", speed = 80, elementId = 2),
				skill = damagingSkill(skillId = 163, name = "劈开", elementId = 1, power = 70, slicingBased = true),
				rules = neutralRules(),
				randomPercent = 100,
			),
		)
		val untagged = calculator.calculate(
			BattleDamageRequest(
				attacker = attacker,
				defender = participant("defender", speed = 80, elementId = 2),
				skill = damagingSkill(skillId = 1, name = "普通切名技能", elementId = 1, power = 70, slicingBased = false),
				rules = neutralRules(),
				randomPercent = 100,
			),
		)

		fixture.assertNamed("slicing-based-ability-boosts-slicing-tagged-skill-damage")
		assertEquals(1.5, tagged.abilityMultiplier)
		assertEquals(72, tagged.amount)
		assertEquals(1.0, untagged.abilityMultiplier)
		assertEquals(48, untagged.amount)
	}

	@Test
	fun `contact based ability boosts only contact skill damage`() {
		val fixture = publicBattleRuleFixture(
			name = "contact-based-ability-boosts-contact-skill-damage",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
				"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
			),
			inputSummary = "使用者拥有接触类技能伤害提升特性，分别使用接触和非接触的一般属性 40 威力物理技能。",
			expectedSummary = "接触技能在最终伤害中获得 1.3 倍特性倍率，非接触技能保持原伤害。",
		)
		val attacker = participant(
			"attacker",
			speed = 100,
			elementId = 1,
			abilityEffects = listOf(BattleAbilityEffect.ContactBasedSkillDamageBoost()),
		)

		val contact = calculator.calculate(
			BattleDamageRequest(
				attacker = attacker,
				defender = participant("defender", speed = 80, elementId = 2),
				skill = damagingSkill(elementId = 1, power = 40, makesContact = true),
				rules = neutralRules(),
				randomPercent = 100,
			),
		)
		val nonContact = calculator.calculate(
			BattleDamageRequest(
				attacker = attacker,
				defender = participant("defender", speed = 80, elementId = 2),
				skill = damagingSkill(elementId = 1, power = 40, makesContact = false),
				rules = neutralRules(),
				randomPercent = 100,
			),
		)

		fixture.assertNamed("contact-based-ability-boosts-contact-skill-damage")
		assertEquals(1.3, contact.abilityMultiplier)
		assertEquals(37, contact.amount)
		assertEquals(1.0, nonContact.abilityMultiplier)
		assertEquals(28, nonContact.amount)
	}

	@Test
	fun `sound based ability boosts only sound skill damage`() {
		val fixture = publicBattleRuleFixture(
			name = "sound-based-ability-boosts-sound-skill-damage",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
				"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
			),
			inputSummary = "使用者拥有声音类技能伤害提升特性，分别使用声音和非声音的一般属性 40 威力特殊技能。",
			expectedSummary = "声音技能在最终伤害中获得 1.3 倍特性倍率，非声音技能保持原伤害。",
		)
		val attacker = participant(
			"attacker",
			speed = 100,
			elementId = 1,
			abilityEffects = listOf(BattleAbilityEffect.SoundBasedSkillDamageBoost()),
		)

		val sound = calculator.calculate(
			BattleDamageRequest(
				attacker = attacker,
				defender = participant("defender", speed = 80, elementId = 2),
				skill = damagingSkill(
					elementId = 1,
					damageClass = BattleDamageClass.SPECIAL,
					power = 40,
					soundBased = true,
				),
				rules = neutralRules(),
				randomPercent = 100,
			),
		)
		val nonSound = calculator.calculate(
			BattleDamageRequest(
				attacker = attacker,
				defender = participant("defender", speed = 80, elementId = 2),
				skill = damagingSkill(
					elementId = 1,
					damageClass = BattleDamageClass.SPECIAL,
					power = 40,
					soundBased = false,
				),
				rules = neutralRules(),
				randomPercent = 100,
			),
		)

		fixture.assertNamed("sound-based-ability-boosts-sound-skill-damage")
		assertEquals(1.3, sound.abilityMultiplier)
		assertEquals(37, sound.amount)
		assertEquals(1.0, nonSound.abilityMultiplier)
		assertEquals(28, nonSound.amount)
	}

	@Test
	fun `sound based defensive ability reduces sound skill damage unless target ability is ignored`() {
		val fixture = publicBattleRuleFixture(
			name = "sound-based-defensive-ability-reduces-sound-skill-damage",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
				"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
			),
			inputSummary = "目标拥有声音类技能伤害减免特性，攻击方使用声音类一般属性 40 威力特殊技能。",
			expectedSummary = "未绕过目标特性时声音伤害按 0.5 倍降低；本次技能忽略目标特性时该减伤不生效。",
		)
		val defender = participant(
			"defender",
			speed = 80,
			elementId = 2,
			abilityEffects = listOf(BattleAbilityEffect.SoundBasedSkillDamageReduction()),
		)
		val request = BattleDamageRequest(
			attacker = participant("attacker", speed = 100, elementId = 1),
			defender = defender,
			skill = damagingSkill(
				elementId = 1,
				damageClass = BattleDamageClass.SPECIAL,
				power = 40,
				soundBased = true,
			),
			rules = neutralRules(),
			randomPercent = 100,
		)

		val reduced = calculator.calculate(request)
		val ignored = calculator.calculate(request.copy(ignoreDefenderAbilityEffects = true))

		fixture.assertNamed("sound-based-defensive-ability-reduces-sound-skill-damage")
		assertEquals(0.5, reduced.abilityMultiplier)
		assertEquals(14, reduced.amount)
		assertEquals(1.0, ignored.abilityMultiplier)
		assertEquals(28, ignored.amount)
	}

	@Test
	fun `sun boosts fire damage and weakens water damage`() {
		val fixture = publicBattleRuleFixture(
			name = "sun-boosts-fire-and-weakens-water-damage",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
				"https://bulbapedia.bulbagarden.net/wiki/Harsh_sunlight",
			),
			inputSummary = "晴天环境下分别计算火属性技能和水属性技能的普通伤害。",
			expectedSummary = "火属性伤害使用 1.5 倍天气倍率，水属性伤害使用 0.5 倍天气倍率。",
		)
		val rules = neutralRules().copy(fireElementId = 10, waterElementId = 11)
		val sun = BattleEnvironment(weather = BattleWeather.SUN)

		val fireResult = calculator.calculate(
			BattleDamageRequest(
				attacker = participant("fire-attacker", speed = 100, elementId = 10),
				defender = participant("defender", speed = 80, elementId = 1),
				skill = damagingSkill(elementId = 10, power = 40),
				rules = rules,
				environment = sun,
				randomPercent = 100,
			),
		)
		val waterResult = calculator.calculate(
			BattleDamageRequest(
				attacker = participant("water-attacker", speed = 100, elementId = 11),
				defender = participant("defender", speed = 80, elementId = 1),
				skill = damagingSkill(elementId = 11, power = 40),
				rules = rules,
				environment = sun,
				randomPercent = 100,
			),
		)

		fixture.assertNamed("sun-boosts-fire-and-weakens-water-damage")
		assertEquals(1.5, fireResult.weatherMultiplier)
		assertEquals(42, fireResult.amount)
		assertEquals(0.5, waterResult.weatherMultiplier)
		assertEquals(14, waterResult.amount)
	}

	@Test
	fun `rain boosts water damage and weakens fire damage`() {
		val fixture = publicBattleRuleFixture(
			name = "rain-boosts-water-and-weakens-fire-damage",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
				"https://bulbapedia.bulbagarden.net/wiki/Rain",
			),
			inputSummary = "下雨环境下分别计算水属性技能和火属性技能的普通伤害。",
			expectedSummary = "水属性伤害使用 1.5 倍天气倍率，火属性伤害使用 0.5 倍天气倍率。",
		)
		val rules = neutralRules().copy(fireElementId = 10, waterElementId = 11)
		val rain = BattleEnvironment(weather = BattleWeather.RAIN)

		val waterResult = calculator.calculate(
			BattleDamageRequest(
				attacker = participant("water-attacker", speed = 100, elementId = 11),
				defender = participant("defender", speed = 80, elementId = 1),
				skill = damagingSkill(elementId = 11, power = 40),
				rules = rules,
				environment = rain,
				randomPercent = 100,
			),
		)
		val fireResult = calculator.calculate(
			BattleDamageRequest(
				attacker = participant("fire-attacker", speed = 100, elementId = 10),
				defender = participant("defender", speed = 80, elementId = 1),
				skill = damagingSkill(elementId = 10, power = 40),
				rules = rules,
				environment = rain,
				randomPercent = 100,
			),
		)

		fixture.assertNamed("rain-boosts-water-and-weakens-fire-damage")
		assertEquals(1.5, waterResult.weatherMultiplier)
		assertEquals(42, waterResult.amount)
		assertEquals(0.5, fireResult.weatherMultiplier)
		assertEquals(14, fireResult.amount)
	}

	@Test
	fun `weather power multiplier modifies base power before damage formula`() {
		val fixture = publicBattleRuleFixture(
			name = "weather-power-multiplier-modifies-base-power",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
				"https://bulbapedia.bulbagarden.net/wiki/Weather",
			),
			inputSummary = "下雨环境下，技能资料声明威力按 0.5 倍参与普通伤害公式。",
			expectedSummary = "伤害计算器先把基础威力折半，再进入等级、攻防和倍率公式。",
		)

		val result = calculator.calculate(
			BattleDamageRequest(
				attacker = participant("weather-power-user", speed = 100, elementId = 2),
				defender = participant("defender", speed = 80, elementId = 1),
				skill = damagingSkill(
					elementId = 1,
					power = 120,
					powerMultipliersByWeather = mapOf(BattleWeather.RAIN to 0.5),
				),
				rules = neutralRules(),
				environment = BattleEnvironment(weather = BattleWeather.RAIN),
				randomPercent = 100,
			),
		)

		fixture.assertNamed("weather-power-multiplier-modifies-base-power")
		assertEquals(28, result.baseDamage)
		assertEquals(28, result.amount)
	}

	@Test
	fun `sandstorm boosts rock special defense before special damage`() {
		val fixture = publicBattleRuleFixture(
			name = "sandstorm-boosts-rock-special-defense",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
				"https://bulbapedia.bulbagarden.net/wiki/Sandstorm_(weather_condition)",
			),
			inputSummary = "沙暴环境下，普通特殊技能命中岩属性目标。",
			expectedSummary = "目标特防按 1.5 倍参与伤害公式，最终伤害低于普通天气。",
		)

		val result = calculator.calculate(
			BattleDamageRequest(
				attacker = participant("attacker", speed = 100, elementId = 2),
				defender = participant("rock-defender", speed = 80, elementId = 6),
				skill = damagingSkill(elementId = 1, damageClass = BattleDamageClass.SPECIAL, power = 40),
				rules = neutralRules(),
				environment = BattleEnvironment(weather = BattleWeather.SANDSTORM),
				randomPercent = 100,
			),
		)

		fixture.assertNamed("sandstorm-boosts-rock-special-defense")
		assertEquals(13, result.baseDamage)
		assertEquals(13, result.amount)
	}

	@Test
	fun `snow boosts ice physical defense before physical damage`() {
		val fixture = publicBattleRuleFixture(
			name = "snow-boosts-ice-physical-defense",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
				"https://bulbapedia.bulbagarden.net/wiki/Snow",
			),
			inputSummary = "雪景环境下，普通物理技能命中冰属性目标。",
			expectedSummary = "目标物防按 1.5 倍参与伤害公式，最终伤害低于普通天气。",
		)

		val result = calculator.calculate(
			BattleDamageRequest(
				attacker = participant("attacker", speed = 100, elementId = 2),
				defender = participant("ice-defender", speed = 80, elementId = 15),
				skill = damagingSkill(elementId = 1, damageClass = BattleDamageClass.PHYSICAL, power = 40),
				rules = neutralRules(),
				environment = BattleEnvironment(weather = BattleWeather.SNOW),
				randomPercent = 100,
			),
		)

		fixture.assertNamed("snow-boosts-ice-physical-defense")
		assertEquals(13, result.baseDamage)
		assertEquals(13, result.amount)
	}

	@Test
	fun `grassy terrain boosts grounded grass damage`() {
		val fixture = publicBattleRuleFixture(
			name = "grassy-terrain-boosts-grounded-grass-damage",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
				"https://bulbapedia.bulbagarden.net/wiki/Grassy_Terrain_(move)",
			),
			inputSummary = "青草场地中，接地草属性成员使用草属性技能。",
			expectedSummary = "伤害公式额外应用 1.3 倍场地倍率；非接地使用者不会获得该倍率。",
		)
		val rules = neutralRules().copy(grassElementId = 12)
		val grassy = BattleEnvironment(terrain = BattleTerrain.GRASSY)

		val groundedResult = calculator.calculate(
			BattleDamageRequest(
				attacker = participant("grounded-grass", speed = 100, elementId = 12),
				defender = participant("defender", speed = 80, elementId = 1),
				skill = damagingSkill(elementId = 12, power = 40),
				rules = rules,
				environment = grassy,
				randomPercent = 100,
			),
		)
		val ungroundedResult = calculator.calculate(
			BattleDamageRequest(
				attacker = participant("ungrounded-grass", speed = 100, elementId = 12, grounded = false),
				defender = participant("defender", speed = 80, elementId = 1),
				skill = damagingSkill(elementId = 12, power = 40),
				rules = rules,
				environment = grassy,
				randomPercent = 100,
			),
		)

		fixture.assertNamed("grassy-terrain-boosts-grounded-grass-damage")
		assertEquals(1.3, groundedResult.terrainMultiplier)
		assertEquals(37, groundedResult.amount)
		assertEquals(1.0, ungroundedResult.terrainMultiplier)
		assertEquals(28, ungroundedResult.amount)
	}

	@Test
	fun `grassy terrain weakens tagged ground shaking moves against grounded targets`() {
		val fixture = publicBattleRuleFixture(
			name = "grassy-terrain-weakens-tagged-ground-shaking-moves-against-grounded-targets",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
				"https://bulbapedia.bulbagarden.net/wiki/Grassy_Terrain_(move)",
			),
			inputSummary = "青草场地中，带震动削弱标签的地面物理技能分别命中接地和非接地目标。",
			expectedSummary = "接地目标受到 0.5 倍场地倍率伤害，非接地目标不受该削弱影响。",
		)
		val grassy = BattleEnvironment(terrain = BattleTerrain.GRASSY)
		val shakingMove = damagingSkill(
			elementId = 5,
			power = 100,
			weakenedByGrassyTerrain = true,
		)

		val groundedResult = calculator.calculate(
			BattleDamageRequest(
				attacker = participant("attacker", speed = 100, elementId = 1),
				defender = participant("grounded-defender", speed = 80, elementId = 1),
				skill = shakingMove,
				rules = neutralRules(),
				environment = grassy,
				randomPercent = 100,
			),
		)
		val ungroundedResult = calculator.calculate(
			BattleDamageRequest(
				attacker = participant("attacker", speed = 100, elementId = 1),
				defender = participant("ungrounded-defender", speed = 80, elementId = 1, grounded = false),
				skill = shakingMove,
				rules = neutralRules(),
				environment = grassy,
				randomPercent = 100,
			),
		)

		fixture.assertNamed("grassy-terrain-weakens-tagged-ground-shaking-moves-against-grounded-targets")
		assertEquals(0.5, groundedResult.terrainMultiplier)
		assertEquals(23, groundedResult.amount)
		assertEquals(1.0, ungroundedResult.terrainMultiplier)
		assertEquals(46, ungroundedResult.amount)
	}
}
