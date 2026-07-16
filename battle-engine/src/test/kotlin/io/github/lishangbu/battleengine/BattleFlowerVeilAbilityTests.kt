package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleStatStageEffect
import io.github.lishangbu.battleengine.model.BattleStatusApplication
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

class BattleFlowerVeilAbilityTests {
	@Test
	fun `flower veil protects active grass allies from status and stat drops`() {
		val grassElementId = 12L
		val skill = damagingSkill(skillId = 1051, damageClass = BattleDamageClass.STATUS, power = null).copy(
			statusApplications = listOf(
				BattleStatusApplication(BattleMajorStatus.POISON, BattleEffectTarget.TARGET, 100),
			),
			statStageEffects = listOf(
				BattleStatStageEffect(BattleStat.ATTACK, BattleEffectTarget.TARGET, -1, 100),
			),
		)
		val effects = listOf(
			BattleAbilityEffect.SideElementMajorStatusImmunity(grassElementId, BattleMajorStatus.entries.toSet()),
			BattleAbilityEffect.SideElementStatDropImmunity(grassElementId),
		)
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(
				doubleInitialState(
					firstA = participant("grass-target", 80, elementId = grassElementId),
					firstB = participant("veil-holder", 70, abilityEffects = effects),
					secondA = participant("attacker", 100, skill = skill),
					secondB = participant("opponent-ally", 60),
				),
			),
			listOf(BattleAction.UseSkill("attacker", skill.skillId, "grass-target")),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals(null, resolved.participant("grass-target")?.majorStatus)
		assertEquals(0, resolved.participant("grass-target")?.statStage(BattleStat.ATTACK))
	}
}
