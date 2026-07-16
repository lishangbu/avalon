package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleStatusApplication
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

class BattleMajorStatusReflectionAbilityTests {
	@Test
	fun `synchronize reflects an opponent inflicted burn`() {
		val statusSkill = damagingSkill(
			skillId = 1084,
			damageClass = BattleDamageClass.STATUS,
			power = null,
			statusApplications = listOf(
				BattleStatusApplication(
					status = BattleMajorStatus.BURN,
					target = BattleEffectTarget.TARGET,
					chancePercent = 100,
				),
			),
		)
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("attacker", 100, skill = statusSkill),
					second = participant(
						"synchronize-holder",
						50,
						abilityEffects = listOf(BattleAbilityEffect.OpponentMajorStatusReflection()),
					),
				),
			),
			listOf(BattleAction.UseSkill("attacker", statusSkill.skillId, "synchronize-holder")),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals(BattleMajorStatus.BURN, resolved.participant("synchronize-holder")?.majorStatus)
		assertEquals(BattleMajorStatus.BURN, resolved.participant("attacker")?.majorStatus)
	}
}
