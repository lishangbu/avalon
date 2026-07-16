package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class BattleUnburdenAbilityTests {
	@Test
	fun `losing an item doubles speed until the holder leaves`() {
		val holderSkill = damagingSkill(skillId = 911)
		val opponentSkill = damagingSkill(skillId = 912)
		val holder = participant(
			"holder",
			50,
			skill = holderSkill,
			itemId = 1,
			abilityEffects = listOf(BattleAbilityEffect.ItemLostSpeedMultiplier(2.0)),
		).consumeHeldItem()
		val resolved = BattleEngine().let { engine ->
			engine.resolveTurn(
				engine.start(initialState(first = holder, second = participant("opponent", 80, skill = opponentSkill))),
				listOf(
					BattleAction.UseSkill("holder", holderSkill.skillId, "opponent"),
					BattleAction.UseSkill("opponent", opponentSkill.skillId, "holder"),
				),
				ScriptedBattleRandom(listOf(1, 15, 1, 15)),
			)
		}

		assertEquals("holder", resolved.events.filterIsInstance<BattleEvent.SkillUsed>().first().actorId)
		assertFalse(holder.leaveBattlefield().itemLostSinceEntering)
	}
}
