package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleStatusApplication
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

class BattleMyceliumMightAbilityTests {
	@Test
	fun `status skills move last and ignore the targets defensive ability`() {
		val poisonSkill = damagingSkill(
			skillId = 5300,
			damageClass = BattleDamageClass.STATUS,
			power = null,
			statusApplications = listOf(
				BattleStatusApplication(BattleMajorStatus.POISON, BattleEffectTarget.TARGET, 100),
			),
		)
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant(
						"mycelium-holder",
						100,
						skill = poisonSkill,
						abilityEffects = listOf(BattleAbilityEffect.StatusSkillMovesLastAndIgnoresTargetAbility()),
					),
					second = participant(
						"defender",
						50,
						skill = poisonSkill,
						abilityEffects = listOf(BattleAbilityEffect.OpponentStatusSkillImmunity()),
					),
				),
			),
			listOf(
				BattleAction.UseSkill("mycelium-holder", poisonSkill.skillId, "defender"),
				BattleAction.UseSkill("defender", poisonSkill.skillId, "mycelium-holder"),
			),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals(
			listOf("defender", "mycelium-holder"),
			resolved.events.filterIsInstance<BattleEvent.SkillUsed>().map { it.actorId },
		)
		assertEquals(BattleMajorStatus.POISON, resolved.participant("defender")?.majorStatus)
	}

	@Test
	fun `damaging skills keep normal speed order and do not ignore defensive abilities`() {
		val skill = damagingSkill(skillId = 5301)
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant(
						"mycelium-holder",
						100,
						skill = skill,
						abilityEffects = listOf(BattleAbilityEffect.StatusSkillMovesLastAndIgnoresTargetAbility()),
					),
					second = participant(
						"defender",
						50,
						skill = skill,
						abilityEffects = listOf(BattleAbilityEffect.NonSuperEffectiveDamageImmunity()),
					),
				),
			),
			listOf(BattleAction.UseSkill("mycelium-holder", skill.skillId, "defender")),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals("mycelium-holder", resolved.events.filterIsInstance<BattleEvent.SkillUsed>().single().actorId)
		assertEquals(100, resolved.participant("defender")?.currentHp)
	}
}
