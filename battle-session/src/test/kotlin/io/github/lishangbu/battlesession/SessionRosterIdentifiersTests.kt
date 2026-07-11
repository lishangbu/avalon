package io.github.lishangbu.battlesession

import kotlin.test.Test
import kotlin.test.assertEquals

class SessionRosterIdentifiersTests {
	@Test
	fun `阵容标识按双方与成员顺序稳定分配`() {
		val layout = SessionRosterIdentifiers().assign(
			sides = listOf(
				SessionRosterSideInput(participantCount = 2, activeParticipantIndexes = listOf(1)),
				SessionRosterSideInput(participantCount = 3, activeParticipantIndexes = listOf(0)),
			),
			activeParticipantsPerSide = 1,
		)

		assertEquals(
			listOf(
				SessionRosterSideLayout(
					sideId = "side-1",
					actorIds = listOf("side-1-actor-1", "side-1-actor-2"),
					activeActorIds = listOf("side-1-actor-2"),
				),
				SessionRosterSideLayout(
					sideId = "side-2",
					actorIds = listOf("side-2-actor-1", "side-2-actor-2", "side-2-actor-3"),
					activeActorIds = listOf("side-2-actor-1"),
				),
			),
			layout.sides,
		)
	}
}
