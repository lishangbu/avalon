package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.ElementEffectivenessChart
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertTrue

class BattleTypeImmunityAbilityTests {
	@Test
	fun `wonder guard blocks neutral damage but permits super effective damage`() {
		val neutralSkill = damagingSkill(skillId = 921, elementId = 1)
		val superSkill = damagingSkill(skillId = 922, elementId = 10)
		val rules = neutralRules().copy(elementChart = ElementEffectivenessChart(mapOf(10L to mapOf(2L to 2.0))))
		val holder = participant(
			"holder",
			50,
			elementId = 2,
			abilityEffects = listOf(BattleAbilityEffect.NonSuperEffectiveDamageImmunity()),
		)
		val blocked = resolve(neutralSkill, holder, rules, emptyList())
		val damaged = resolve(superSkill, holder, rules, listOf(1, 15))

		assertTrue(blocked.events.any { it is BattleEvent.SkillBlockedByAbility })
		assertTrue(damaged.events.any { it is BattleEvent.DamageApplied })
	}

	@Test
	fun `scrappy bypasses normal immunity supplied by ghost typing`() {
		val skill = damagingSkill(skillId = 923, elementId = 1)
		val rules = neutralRules().copy(elementChart = ElementEffectivenessChart(mapOf(1L to mapOf(8L to 0.0))))
		val target = participant("ghost", 50, elementId = 8)
		val resolved = resolve(
			skill,
			target,
			rules,
			listOf(1, 15),
			listOf(BattleAbilityEffect.ElementTypeImmunityBypass(setOf(1, 2))),
		)

		assertTrue(resolved.events.any { it is BattleEvent.DamageApplied })
	}

	private fun resolve(
		skill: io.github.lishangbu.battleengine.model.BattleSkillSlot,
		target: io.github.lishangbu.battleengine.model.BattleParticipant,
		rules: io.github.lishangbu.battleengine.model.BattleRuleSnapshot,
		random: List<Int>,
		attackerEffects: List<BattleAbilityEffect> = emptyList(),
	) = BattleEngine().let { engine ->
		engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("attacker", 100, skill = skill, abilityEffects = attackerEffects),
					second = target,
					rules = rules,
				),
			),
			listOf(BattleAction.UseSkill("attacker", skill.skillId, target.actorId)),
			ScriptedBattleRandom(random),
		)
	}
}
