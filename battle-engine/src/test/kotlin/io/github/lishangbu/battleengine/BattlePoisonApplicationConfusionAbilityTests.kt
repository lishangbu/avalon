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

class BattlePoisonApplicationConfusionAbilityTests {
	@Test
	fun `poison puppeteer confuses an opponent after poisoning it`() {
		val skill = damagingSkill(
			skillId = 1089,
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
						"poison-puppeteer-user",
						100,
						skill = skill,
						abilityEffects = listOf(BattleAbilityEffect.PoisonApplicationConfusion()),
					),
					second = participant("target", 50),
				),
			),
			listOf(BattleAction.UseSkill("poison-puppeteer-user", skill.skillId, "target")),
			ScriptedBattleRandom(listOf(0)),
		)

		assertEquals(BattleMajorStatus.POISON, resolved.participant("target")?.majorStatus)
		assertEquals(2, resolved.participant("target")?.confusionTurnsRemaining)
	}
}
