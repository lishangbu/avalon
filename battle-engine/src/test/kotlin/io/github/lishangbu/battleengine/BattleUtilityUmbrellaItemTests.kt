package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.damage.BattleDamageCalculator
import io.github.lishangbu.battleengine.damage.BattleDamageRequest
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleSkillHpEffect
import io.github.lishangbu.battleengine.model.BattleWeather
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/** 验证万能伞只屏蔽持有者感受到的晴天和雨天天气效果。 */
class BattleUtilityUmbrellaItemTests {
	private val engine = BattleEngine()
	private val umbrella = BattleItemEffect.SunRainEffectImmunity()

	@Test
	fun `umbrella suppresses sun and rain damage effects for attacker or defender`() {
		val calculator = BattleDamageCalculator()
		val fireSkill = damagingSkill(elementId = 10, power = 40)
		val plainAttacker = participant("attacker", speed = 100, elementId = 10, skill = fireSkill)
		val plainDefender = participant("defender", speed = 50)
		val neutral = calculator.calculate(
			BattleDamageRequest(plainAttacker, plainDefender, fireSkill, neutralRules(), randomPercent = 100),
		)
		val attackerProtected = calculator.calculate(
			BattleDamageRequest(
				plainAttacker.copy(itemId = 1181).replaceItemEffects(listOf(umbrella)),
				plainDefender,
				fireSkill,
				neutralRules(),
				environment = BattleEnvironment(weather = BattleWeather.SUN),
				randomPercent = 100,
			),
		)
		val defenderProtected = calculator.calculate(
			BattleDamageRequest(
				plainAttacker,
				plainDefender.copy(itemId = 1181).replaceItemEffects(listOf(umbrella)),
				fireSkill,
				neutralRules(),
				environment = BattleEnvironment(weather = BattleWeather.SUN),
				randomPercent = 100,
			),
		)

		assertEquals(1.0, attackerProtected.weatherMultiplier)
		assertEquals(neutral.amount, attackerProtected.amount)
		assertEquals(1.0, defenderProtected.weatherMultiplier)
		assertEquals(neutral.amount, defenderProtected.amount)
	}

	@Test
	fun `umbrella user ignores rain accuracy and weather ball overrides`() {
		val skill = damagingSkill(
			elementId = 1,
			power = 50,
			accuracy = 70,
			accuracyOverridesByWeather = mapOf(BattleWeather.RAIN to null),
			powerMultipliersByWeather = mapOf(BattleWeather.RAIN to 2.0),
			elementOverridesByWeather = mapOf(BattleWeather.RAIN to 11),
		)
		val state = engine.start(
			initialState(
				first = participant(
					"umbrella-user",
					speed = 100,
					skill = skill,
					itemId = 1181,
					itemEffects = listOf(umbrella),
				),
				second = participant("target", speed = 50),
				environment = BattleEnvironment(weather = BattleWeather.RAIN),
			),
		)
		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("umbrella-user", skill.skillId, "target")),
			ScriptedBattleRandom(listOf(79)),
		)

		assertEquals(80, resolved.events.filterIsInstance<BattleEvent.SkillMissed>().single().accuracyRoll)
		assertEquals(100, resolved.participant("target")?.currentHp)
	}

	@Test
	fun `umbrella user receives default weather sensitive healing`() {
		val skill = damagingSkill(
			damageClass = BattleDamageClass.STATUS,
			power = null,
			hpEffects = listOf(
				BattleSkillHpEffect.SelfHealMaxHpByWeather(
					defaultFraction = BattleSkillHpEffect.HpFraction(1, 2),
					weatherFractions = mapOf(BattleWeather.SUN to BattleSkillHpEffect.HpFraction(2, 3)),
				),
			),
		)
		val state = engine.start(
			initialState(
				first = participant(
					"umbrella-user",
					speed = 100,
					currentHp = 20,
					skill = skill,
					itemId = 1181,
					itemEffects = listOf(umbrella),
				),
				second = participant("target", speed = 50),
				environment = BattleEnvironment(weather = BattleWeather.SUN),
			),
		)
		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("umbrella-user", skill.skillId, "target")),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals(70, resolved.participant("umbrella-user")?.currentHp)
	}
}
