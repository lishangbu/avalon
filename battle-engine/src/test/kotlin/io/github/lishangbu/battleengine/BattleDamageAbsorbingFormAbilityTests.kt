package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleFormPair
import io.github.lishangbu.battleengine.model.BattleFormProfile
import io.github.lishangbu.battleengine.model.BattleWeather
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 验证完整形态吸收伤害、破损代价及天气复原。 */
class BattleDamageAbsorbingFormAbilityTests {
	@Test
	fun `disguise absorbs only the first segment and pays one eighth hp`() {
		val disguised = form(778, defense = 80)
		val busted = form(10143, defense = 80)
		val effect = BattleAbilityEffect.DamageAbsorbingFormChange(
			listOf(BattleFormPair("mimikyu-disguised", "mimikyu-busted")),
			setOf(BattleDamageClass.PHYSICAL, BattleDamageClass.SPECIAL),
			breakHpLossDenominator = 8,
		)
		val skill = damagingSkill(minHits = 2, maxHits = 2)
		val target = participant("mimikyu", 50, creatureId = disguised.creatureId,
			abilityEffects = listOf(effect),
			battleFormProfiles = mapOf("mimikyu-disguised" to disguised, "mimikyu-busted" to busted))
		val engine = BattleEngine()

		val resolved = engine.resolveTurn(
			engine.start(initialState(participant("attacker", 100, skill = skill), target)),
			listOf(BattleAction.UseSkill("attacker", skill.skillId, "mimikyu")),
			ScriptedBattleRandom(listOf(1, 15, 1, 15)),
		)

		assertEquals(busted.creatureId, resolved.participant("mimikyu")?.creatureId)
		assertTrue(requireNotNull(resolved.participant("mimikyu")?.currentHp) < 88)
	}

	@Test
	fun `ice face absorbs physical damage and snow restores the ice form`() {
		val ice = form(875, defense = 110)
		val noice = form(10185, defense = 70)
		val pair = BattleFormPair("eiscue-ice", "eiscue-noice")
		val effects = listOf(
			BattleAbilityEffect.DamageAbsorbingFormChange(listOf(pair), setOf(BattleDamageClass.PHYSICAL)),
			BattleAbilityEffect.WeatherFormRestore(BattleWeather.SNOW, listOf(pair)),
		)
		val skill = damagingSkill()
		val target = participant("eiscue", 50, creatureId = ice.creatureId, abilityEffects = effects,
			battleFormProfiles = mapOf("eiscue-ice" to ice, "eiscue-noice" to noice))
		val engine = BattleEngine()
		val broken = engine.resolveTurn(
			engine.start(initialState(participant("attacker", 100, skill = skill), target)),
			listOf(BattleAction.UseSkill("attacker", skill.skillId, "eiscue")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val snowing = broken.copy(environment = BattleEnvironment(weather = BattleWeather.SNOW))
			.synchronizeWeatherForms()

		assertEquals(noice.creatureId, broken.participant("eiscue")?.creatureId)
		assertEquals(100, broken.participant("eiscue")?.currentHp)
		assertEquals(ice.creatureId, snowing.participant("eiscue")?.creatureId)
	}

	@Test
	fun `weather suppression prevents snow from restoring ice face`() {
		val ice = form(875, defense = 110)
		val noice = form(10185, defense = 70)
		val pair = BattleFormPair("eiscue-ice", "eiscue-noice")
		val skill = damagingSkill()
		val target = participant(
			"eiscue",
			50,
			creatureId = ice.creatureId,
			abilityEffects = listOf(
				BattleAbilityEffect.DamageAbsorbingFormChange(
					listOf(pair),
					setOf(BattleDamageClass.PHYSICAL),
				),
				BattleAbilityEffect.WeatherFormRestore(BattleWeather.SNOW, listOf(pair)),
			),
			battleFormProfiles = mapOf("eiscue-ice" to ice, "eiscue-noice" to noice),
		)
		val suppressor = participant(
			"suppressor",
			100,
			skill = skill,
			abilityEffects = listOf(BattleAbilityEffect.WeatherEffectSuppression()),
		)
		val engine = BattleEngine()
		val broken = engine.resolveTurn(
			engine.start(initialState(suppressor, target)),
			listOf(BattleAction.UseSkill(suppressor.actorId, skill.skillId, target.actorId)),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		val snowing = broken.copy(environment = BattleEnvironment(weather = BattleWeather.SNOW))
			.synchronizeWeatherForms()

		assertEquals(noice.creatureId, snowing.participant(target.actorId)?.creatureId)
	}

	private fun form(creatureId: Long, defense: Int) = BattleFormProfile(
		creatureId, 100, 100, defense, 100, 100, 90, 100, setOf(1),
	)
}
