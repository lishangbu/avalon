package io.github.lishangbu.battlerules

import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battlerules.service.BattleSessionResponseMapper
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BattleSessionResponseMapperTests {
	@Test
	fun `event payload keeps identifier values safe for javascript clients`() {
		val event = BattleEvent.SkillUsed(
			turnNumber = 1,
			actorId = "side-1-actor-1",
			targetActorId = "side-2-actor-1",
			skillId = JAVASCRIPT_UNSAFE_LONG,
			skillName = "test-skill",
		)

		val response = BattleSessionResponseMapper().toEvent(event)

		assertEquals("SkillUsed", response.type)
		assertEquals(JAVASCRIPT_UNSAFE_LONG.toString(), response.payload["skillId"])
		assertEquals("side-1-actor-1", response.payload["actorId"])
	}

	private companion object {
		const val JAVASCRIPT_UNSAFE_LONG = 9_007_199_254_740_993L
	}
}
