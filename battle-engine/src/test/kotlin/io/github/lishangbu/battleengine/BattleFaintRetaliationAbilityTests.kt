package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleFixedDamage
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

class BattleFaintRetaliationAbilityTests {
	@Test
	fun `damp suppresses aftermath while active`() {
		val contactSkill = damagingSkill(
			skillId = 932,
			power = null,
			fixedDamage = BattleFixedDamage.FixedAmount(200),
			makesContact = true,
		)
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(
				doubleInitialState(
					firstA = participant("attacker", 100, skill = contactSkill),
					firstB = participant("attacker-ally", 90),
					secondA = participant(
						"holder",
						50,
						abilityEffects = listOf(
							BattleAbilityEffect.FaintAttackerDamage(
								requiresContact = true,
								attackerMaxHpDenominator = 4,
								suppressedByExplosionSuppression = true,
							),
						),
					),
					secondB = participant(
						"damp-ally",
						40,
						abilityEffects = listOf(BattleAbilityEffect.ExplosionEffectSuppression()),
					),
				),
			),
			listOf(BattleAction.UseSkill("attacker", contactSkill.skillId, "holder")),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals(100, resolved.participant("attacker")?.currentHp)
	}

	@Test
	fun `aftermath and innards out damage the attacker after holder faints`() {
		val contactSkill = damagingSkill(
			skillId = 931,
			power = null,
			fixedDamage = BattleFixedDamage.FixedAmount(200),
			makesContact = true,
		)
		val aftermath = resolve(
			contactSkill,
			participant(
				"holder",
				50,
				abilityEffects = listOf(
					BattleAbilityEffect.FaintAttackerDamage(requiresContact = true, attackerMaxHpDenominator = 4),
				),
			),
		)
		val innardsOut = resolve(
			contactSkill,
			participant(
				"holder",
				50,
				currentHp = 40,
				abilityEffects = listOf(BattleAbilityEffect.FaintAttackerDamage(usesDamageTaken = true)),
			),
		)

		assertEquals(75, aftermath.participant("attacker")?.currentHp)
		assertEquals(60, innardsOut.participant("attacker")?.currentHp)
	}

	private fun resolve(
		skill: io.github.lishangbu.battleengine.model.BattleSkillSlot,
		holder: io.github.lishangbu.battleengine.model.BattleParticipant,
	) = BattleEngine().let { engine ->
		engine.resolveTurn(
			engine.start(initialState(first = participant("attacker", 100, skill = skill), second = holder)),
			listOf(BattleAction.UseSkill("attacker", skill.skillId, "holder")),
			ScriptedBattleRandom(emptyList()),
		)
	}
}
