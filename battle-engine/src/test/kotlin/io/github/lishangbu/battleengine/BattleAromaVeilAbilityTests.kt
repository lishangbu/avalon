package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleVolatileStatus
import io.github.lishangbu.battleengine.model.BattleVolatileStatusApplication
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertTrue

class BattleAromaVeilAbilityTests {
	@Test
	fun `aroma veil protects an active ally from restricted volatile statuses`() {
		val taunt = damagingSkill(skillId = 1011, damageClass = BattleDamageClass.STATUS, power = null).copy(
			volatileStatusApplications = listOf(
				BattleVolatileStatusApplication(BattleVolatileStatus.TAUNT, BattleEffectTarget.TARGET, 100),
			),
		)
		val protectedStatuses = setOf(
			BattleVolatileStatus.TAUNT,
			BattleVolatileStatus.TORMENT,
			BattleVolatileStatus.INFATUATION,
			BattleVolatileStatus.DISABLE,
			BattleVolatileStatus.HEAL_BLOCK,
		)
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(
				doubleInitialState(
					firstA = participant("target", 80),
					firstB = participant(
						"veil-holder",
						70,
						abilityEffects = listOf(BattleAbilityEffect.SideVolatileStatusImmunity(protectedStatuses)),
					),
					secondA = participant("attacker", 100, skill = taunt),
					secondB = participant("opponent-ally", 60),
				),
			),
			listOf(BattleAction.UseSkill("attacker", taunt.skillId, "target")),
			ScriptedBattleRandom(emptyList()),
		)

		assertTrue(resolved.events.filterIsInstance<BattleEvent.VolatileStatusApplicationBlocked>().isNotEmpty())
	}
}
