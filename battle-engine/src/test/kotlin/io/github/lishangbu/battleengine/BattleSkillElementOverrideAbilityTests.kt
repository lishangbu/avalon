package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.model.BattleWeather
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BattleSkillElementOverrideAbilityTests {
	@Test
	fun `ate abilities convert normal skills and apply their damage boost`() {
		val skill = damagingSkill(skillId = 911, elementId = 1, power = 80)
		val effect = BattleAbilityEffect.SkillElementOverride(
			elementId = 2,
			originalElementIds = setOf(1),
			damageMultiplier = 1.2,
		)
		val holder = participant("holder", 100, elementId = 2, skill = skill, abilityEffects = listOf(effect))

		assertEquals(2, skill.effectiveElementId(BattleWeather.NONE, BattleTerrain.NONE, holder))
		assertTrue(damage(skill, listOf(effect), attackerElementId = 2) > damage(skill, attackerElementId = 2))
	}

	@Test
	fun `normalize converts every typed skill while liquid voice converts only sound skills`() {
		val fireSkill = damagingSkill(skillId = 912, elementId = 10, power = 80)
		val soundSkill = damagingSkill(skillId = 913, elementId = 1, power = 80, soundBased = true)
		val normalize = participant(
			"normalize",
			100,
			abilityEffects = listOf(BattleAbilityEffect.SkillElementOverride(elementId = 1, damageMultiplier = 1.2)),
		)
		val liquidVoice = participant(
			"liquid-voice",
			100,
			abilityEffects = listOf(BattleAbilityEffect.SkillElementOverride(elementId = 11, requiresSoundBased = true)),
		)

		assertEquals(1, fireSkill.effectiveElementId(BattleWeather.NONE, BattleTerrain.NONE, normalize))
		assertEquals(11, soundSkill.effectiveElementId(BattleWeather.NONE, BattleTerrain.NONE, liquidVoice))
		assertEquals(10, fireSkill.effectiveElementId(BattleWeather.NONE, BattleTerrain.NONE, liquidVoice))
	}

	private fun damage(
		skill: io.github.lishangbu.battleengine.model.BattleSkillSlot,
		effects: List<BattleAbilityEffect> = emptyList(),
		attackerElementId: Long = 1,
	): Int {
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant(
						"attacker",
						100,
						elementId = attackerElementId,
						skill = skill,
						abilityEffects = effects,
					),
					second = participant("defender", 50),
				),
			),
			listOf(BattleAction.UseSkill("attacker", skill.skillId, "defender")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		return resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount
	}
}
