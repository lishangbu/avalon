package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.damage.BattleDamageCalculator
import io.github.lishangbu.battleengine.damage.BattleDamageRequest
import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleRuleSnapshot
import io.github.lishangbu.battleengine.model.BattleSkillEnvironmentEffect
import io.github.lishangbu.battleengine.model.BattleStrongWeather
import io.github.lishangbu.battleengine.model.BattleStrongWeatherState
import io.github.lishangbu.battleengine.model.BattleWeather
import io.github.lishangbu.battleengine.model.ElementEffectivenessChart
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 验证终结之地、始源之海与德尔塔气流共享的强天气生命周期和战斗规则。 */
class BattleStrongWeatherAbilityTests {
	@Test
	fun `harsh sunlight blocks water damage and strengthens fire damage`() {
		val waterSkill = damagingSkill(skillId = 1, elementId = 11)
		val attacker = participant("attacker", 100, skill = waterSkill)
		val target = participant("target", 50)
		val state = BattleEngine().start(initialState(attacker, target))
			.startStrongWeather("target", BattleStrongWeather.HARSH_SUNLIGHT)

		val blocked = BattleEngine().resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", waterSkill.skillId, "target")),
			ScriptedBattleRandom(emptyList()),
		)
		val fireSkill = damagingSkill(skillId = 2, elementId = 10)
		val fireAttacker = attacker.copy(elementIds = setOf(10), skillSlots = listOf(fireSkill))
		val sunnyState = BattleEngine().start(initialState(fireAttacker, target))
			.startStrongWeather("target", BattleStrongWeather.HARSH_SUNLIGHT)
		val sunnyDamage = calculateDamage(sunnyState, fireAttacker, target, fireSkill)
		val clearDamage = calculateDamage(
			BattleEngine().start(initialState(fireAttacker, target)),
			fireAttacker,
			target,
			fireSkill,
		)

		assertEquals(100, blocked.participant("target")?.currentHp)
		assertEquals(
			"strong-weather-negates-damaging-skill",
			blocked.events.filterIsInstance<BattleEvent.SkillFailed>().single().reason,
		)
		assertTrue(sunnyDamage > clearDamage)
	}

	@Test
	fun `heavy rain blocks fire damage and strengthens water damage`() {
		val fireSkill = damagingSkill(skillId = 3, elementId = 10)
		val attacker = participant("attacker", 100, skill = fireSkill)
		val target = participant("target", 50)
		val state = BattleEngine().start(initialState(attacker, target))
			.startStrongWeather("target", BattleStrongWeather.HEAVY_RAIN)

		val blocked = BattleEngine().resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", fireSkill.skillId, "target")),
			ScriptedBattleRandom(emptyList()),
		)
		val waterSkill = damagingSkill(skillId = 4, elementId = 11)
		val waterAttacker = attacker.copy(elementIds = setOf(11), skillSlots = listOf(waterSkill))
		val rainyState = BattleEngine().start(initialState(waterAttacker, target))
			.startStrongWeather("target", BattleStrongWeather.HEAVY_RAIN)

		assertEquals(100, blocked.participant("target")?.currentHp)
		assertTrue(
			calculateDamage(rainyState, waterAttacker, target, waterSkill) >
				calculateDamage(BattleEngine().start(initialState(waterAttacker, target)), waterAttacker, target, waterSkill),
		)
	}

	@Test
	fun `ordinary weather cannot replace strong weather but another strong weather can`() {
		val source = participant("source", 100)
		val state = BattleEngine().start(initialState(source, participant("target", 50)))
			.startStrongWeather(source.actorId, BattleStrongWeather.HARSH_SUNLIGHT)
		val rainSkill = damagingSkill(
			skillId = 5,
			environmentEffects = listOf(BattleSkillEnvironmentEffect.SetWeather(BattleWeather.RAIN)),
		)

		val afterOrdinaryWeather = BattleEnvironmentEffects().applySkillEffects(
			state,
			source.actorId,
			source.actorId,
			rainSkill,
		)
		val afterStrongWeather = afterOrdinaryWeather.startStrongWeather(
			"target",
			BattleStrongWeather.HEAVY_RAIN,
		)

		assertEquals(BattleStrongWeather.HARSH_SUNLIGHT, afterOrdinaryWeather.environment.strongWeather)
		assertEquals(BattleWeather.NONE, afterOrdinaryWeather.environment.weather)
		assertEquals(BattleStrongWeather.HEAVY_RAIN, afterStrongWeather.environment.strongWeather)
		assertEquals("target", afterStrongWeather.environment.strongWeatherSourceActorId)
	}

	@Test
	fun `strong weather ends when its last source switches out`() {
		val effect = BattleAbilityEffect.SwitchInStrongWeatherChange(BattleStrongWeather.HARSH_SUNLIGHT)
		val source = participant("source", 100, abilityEffects = listOf(effect))
		val reserve = participant("reserve", 80)
		val engine = BattleEngine()
		val started = engine.start(initialState(source, participant("target", 50), firstBench = listOf(reserve)))

		val switched = engine.resolveTurn(
			started,
			listOf(BattleAction.SwitchParticipant(source.actorId, reserve.actorId)),
			ScriptedBattleRandom(emptyList()),
		)

		assertNull(switched.environment.strongWeather)
		assertNull(switched.environment.strongWeatherSourceActorId)
	}

	@Test
	fun `another active holder keeps the same strong weather after source faints`() {
		val effect = BattleAbilityEffect.SwitchInStrongWeatherChange(BattleStrongWeather.HEAVY_RAIN)
		val first = participant("first", 100, abilityEffects = listOf(effect))
		val second = participant("second", 90, abilityEffects = listOf(effect))
		val started = BattleEngine().start(
			doubleInitialState(first, second, participant("opponent-a", 50), participant("opponent-b", 40)),
		)
		val sourceId = started.environment.strongWeatherSourceActorId ?: error("strong weather source missing")
		val faintedSource = started.participant(sourceId)?.copy(currentHp = 0) ?: error("source missing")

		val synchronized = started.replaceParticipant(faintedSource).synchronizeStrongWeather()

		assertEquals(BattleStrongWeather.HEAVY_RAIN, synchronized.environment.strongWeather)
		assertTrue(synchronized.environment.strongWeatherSourceActorId in setOf("first", "second") - sourceId)
	}

	@Test
	fun `strong winds remove only flying type weakness contribution`() {
		val rules = strongWindsRules()
		val attacker = participant("attacker", 100, elementId = 13)
		val flying = participant("flying", 50, elementId = 2)
		val flyingAndWater = flying.copy(actorId = "flying-water", elementIds = setOf(2, 11))
		val water = participant("water", 40, elementId = 11)
		val strongWinds = BattleEnvironment(
			strongWeatherState = BattleStrongWeatherState(BattleStrongWeather.STRONG_WINDS, flying.actorId),
		)

		assertEquals(1.0, effectiveTypeEffectiveness(rules, 13, attacker, flying, strongWinds))
		assertEquals(2.0, effectiveTypeEffectiveness(rules, 13, attacker, flyingAndWater, strongWinds))
		assertEquals(2.0, effectiveTypeEffectiveness(rules, 13, attacker, water, strongWinds))
	}

	private fun calculateDamage(
		state: io.github.lishangbu.battleengine.model.BattleState,
		attacker: io.github.lishangbu.battleengine.model.BattleParticipant,
		defender: io.github.lishangbu.battleengine.model.BattleParticipant,
		skill: io.github.lishangbu.battleengine.model.BattleSkillSlot,
	): Int = BattleDamageCalculator().calculate(
		BattleDamageRequest(
			attacker = attacker,
			defender = defender,
			skill = skill,
			rules = state.rules,
			environment = state.effectiveEnvironmentFor(attacker),
			randomPercent = 100,
		),
	).amount

	private fun strongWindsRules(): BattleRuleSnapshot = BattleRuleSnapshot(
		elementChart = ElementEffectivenessChart(
			mapOf(13L to mapOf(2L to 2.0, 11L to 2.0)),
		),
		elementIds = neutralRules().elementIds + ("flying" to 2L),
	)
}
