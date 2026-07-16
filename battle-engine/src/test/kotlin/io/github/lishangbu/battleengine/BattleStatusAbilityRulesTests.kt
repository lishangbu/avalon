package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleStatusApplication
import io.github.lishangbu.battleengine.model.BattleWeather
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BattleStatusAbilityRulesTests {
	@Test
	fun `leaf guard blocks status in sun and good as gold blocks opponent status skills`() {
		val poisonSkill = damagingSkill(
			skillId = 871,
			damageClass = BattleDamageClass.STATUS,
			power = null,
			statusApplications = listOf(
				BattleStatusApplication(BattleMajorStatus.POISON, BattleEffectTarget.TARGET, 100),
			),
		)
		val holder = participant(
			"holder",
			50,
			abilityEffects = listOf(
				BattleAbilityEffect.MajorStatusImmunity(BattleMajorStatus.entries.toSet(), BattleWeather.SUN),
				BattleAbilityEffect.OpponentStatusSkillImmunity(),
			),
		)
		val resolved = BattleEngine().let { engine ->
			engine.resolveTurn(
				engine.start(
					initialState(
						first = participant("attacker", 100, skill = poisonSkill),
						second = holder,
						environment = BattleEnvironment(weather = BattleWeather.SUN),
					),
				),
				listOf(BattleAction.UseSkill("attacker", poisonSkill.skillId, "holder")),
				ScriptedBattleRandom(emptyList()),
			)
		}

		assertEquals(null, resolved.participant("holder")?.majorStatus)
		assertTrue(resolved.events.any { it is BattleEvent.SkillBlockedByAbility })
	}

	@Test
	fun `merciless guarantees a critical hit against a poisoned target`() {
		val skill = damagingSkill(skillId = 872)
		val target = participant("target", 50).copy(majorStatus = BattleMajorStatus.POISON)
		val resolved = BattleEngine().let { engine ->
			engine.resolveTurn(
				engine.start(
					initialState(
						first = participant(
							"attacker",
							100,
							skill = skill,
							abilityEffects = listOf(
								BattleAbilityEffect.MajorStatusGuaranteedCriticalHit(
									setOf(BattleMajorStatus.POISON, BattleMajorStatus.BAD_POISON),
								),
							),
						),
						second = target,
					),
				),
				listOf(BattleAction.UseSkill("attacker", skill.skillId, "target")),
				ScriptedBattleRandom(listOf(15)),
			)
		}

		assertTrue(resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().criticalHit)
	}

	@Test
	fun `poison touch applies poison only after contact damage and a successful roll`() {
		val skill = damagingSkill(skillId = 873, makesContact = true)
		val resolved = BattleEngine().let { engine ->
			engine.resolveTurn(
				engine.start(
					initialState(
						first = participant(
							"attacker",
							100,
							skill = skill,
							abilityEffects = listOf(
								BattleAbilityEffect.DealtDamageMajorStatusChance(BattleMajorStatus.POISON, 30, true),
							),
						),
						second = participant("target", 50),
					),
				),
				listOf(BattleAction.UseSkill("attacker", skill.skillId, "target")),
				ScriptedBattleRandom(listOf(1, 15, 0)),
			)
		}

		assertEquals(BattleMajorStatus.POISON, resolved.participant("target")?.majorStatus)
	}
}
