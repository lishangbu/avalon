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
import kotlin.test.assertTrue

class BattleAlwaysTreatedAsleepAbilityTests {
	@Test
	fun `comatose is treated as statused while blocking actual major status`() {
		val poisonSkill = damagingSkill(
			skillId = 4600,
			damageClass = BattleDamageClass.STATUS,
			power = null,
			statusApplications = listOf(
				BattleStatusApplication(BattleMajorStatus.POISON, BattleEffectTarget.TARGET, 100),
			),
		)
		val holder = participant(
			"holder",
			50,
			abilityEffects = listOf(BattleAbilityEffect.AlwaysTreatedAsleep()),
		)
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(initialState(first = participant("attacker", 100, skill = poisonSkill), second = holder)),
			listOf(BattleAction.UseSkill("attacker", poisonSkill.skillId, "holder")),
			ScriptedBattleRandom(emptyList()),
		)

		val resolvedHolder = requireNotNull(resolved.participant("holder"))
		assertEquals(null, resolvedHolder.majorStatus)
		assertTrue(resolvedHolder.hasEffectiveMajorStatus())
	}
}
