package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

class BattleFirstSkillElementChangeAbilityTests {
	@Test
	fun `protean changes element only for the first different element skill after switch in`() {
		val fireSkill = damagingSkill(skillId = 1086, elementId = 10)
		val waterSkill = damagingSkill(skillId = 1087, elementId = 11)
		val engine = BattleEngine()
		val started = engine.start(
			initialState(
				first = participant(
					"protean-user",
					100,
					skill = fireSkill,
					abilityEffects = listOf(BattleAbilityEffect.FirstSkillElementChangeSinceSwitchIn()),
				).copy(skillSlots = listOf(fireSkill, waterSkill)),
				second = participant("target", 50),
			),
		)
		val afterFire = engine.resolveTurn(
			started,
			listOf(BattleAction.UseSkill("protean-user", fireSkill.skillId, "target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val afterWater = engine.resolveTurn(
			afterFire,
			listOf(BattleAction.UseSkill("protean-user", waterSkill.skillId, "target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		assertEquals(setOf(10L), afterFire.participant("protean-user")?.elementIds)
		assertEquals(setOf(10L), afterWater.participant("protean-user")?.elementIds)
		assertEquals(1, afterWater.events.filterIsInstance<BattleEvent.AbilityElementsChanged>().size)
	}
}
