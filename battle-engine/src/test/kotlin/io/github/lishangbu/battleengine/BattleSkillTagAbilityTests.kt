package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleSkillHpEffect

class BattleSkillTagAbilityTests {
	@Test
	fun `mega launcher also boosts pulse based target healing`() {
		val healPulse = damagingSkill(
			skillId = 1024,
			damageClass = BattleDamageClass.STATUS,
			power = null,
			hpEffects = listOf(BattleSkillHpEffect.TargetHealMaxHpFraction(1, 2)),
		).copy(pulseBased = true)
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant(
						"healer",
						100,
						skill = healPulse,
						abilityEffects = listOf(BattleAbilityEffect.PulseBasedSkillDamageBoost(1.5)),
					),
					second = participant("target", 50, currentHp = 10),
				),
			),
			listOf(BattleAction.UseSkill("healer", healPulse.skillId, "target")),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals(85, resolved.participant("target")?.currentHp)
	}

	@Test
	fun `bulletproof blocks projectile skills`() {
		val skill = damagingSkill(skillId = 1021, power = 40).copy(projectileBased = true)
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("attacker", 100, skill = skill),
					second = participant(
						"holder",
						50,
						abilityEffects = listOf(BattleAbilityEffect.ProjectileSkillImmunity()),
					),
				),
			),
			listOf(BattleAction.UseSkill("attacker", skill.skillId, "holder")),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals(100, resolved.participant("holder")?.currentHp)
		assertEquals(1, resolved.events.filterIsInstance<BattleEvent.SkillBlockedByAbility>().size)
	}

	@Test
	fun `mega launcher and strong jaw boost their tagged skills`() {
		val pulseSkill = damagingSkill(skillId = 1022, power = 40).copy(pulseBased = true)
		val biteSkill = damagingSkill(skillId = 1023, power = 40).copy(biteBased = true)

		assertTrue(
			damage(pulseSkill, listOf(BattleAbilityEffect.PulseBasedSkillDamageBoost(1.5))) > damage(pulseSkill),
		)
		assertTrue(
			damage(biteSkill, listOf(BattleAbilityEffect.BiteBasedSkillDamageBoost(1.5))) > damage(biteSkill),
		)
	}

	private fun damage(
		skill: io.github.lishangbu.battleengine.model.BattleSkillSlot,
		effects: List<BattleAbilityEffect> = emptyList(),
	): Int {
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("attacker", 100, skill = skill, abilityEffects = effects),
					second = participant("defender", 50),
				),
			),
			listOf(BattleAction.UseSkill("attacker", skill.skillId, "defender")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		return resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount
	}
}
