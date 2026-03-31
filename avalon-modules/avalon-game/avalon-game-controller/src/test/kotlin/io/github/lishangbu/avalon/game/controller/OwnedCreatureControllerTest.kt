package io.github.lishangbu.avalon.game.controller

import io.github.lishangbu.avalon.game.service.player.CreatureStorageBoxView
import io.github.lishangbu.avalon.game.service.player.OwnedCreatureQueryService
import io.github.lishangbu.avalon.game.service.player.OwnedCreatureSummaryView
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.time.Instant

class OwnedCreatureControllerTest {
    @Test
    fun listOwnedCreatures_delegatesToService() {
        val service = FakeOwnedCreatureQueryService()
        val controller = OwnedCreatureController(service)

        val result = controller.listOwnedCreatures("1")

        assertSame(service.ownedResult, result)
        assertEquals("1", service.ownedPlayerId)
    }

    @Test
    fun listBoxes_delegatesToService() {
        val service = FakeOwnedCreatureQueryService()
        val controller = OwnedCreatureController(service)

        val result = controller.listBoxes("1")

        assertSame(service.boxesResult, result)
        assertEquals("1", service.boxesPlayerId)
    }

    private class FakeOwnedCreatureQueryService : OwnedCreatureQueryService {
        var ownedPlayerId: String? = null
        var boxesPlayerId: String? = null

        val ownedResult =
            listOf(
                OwnedCreatureSummaryView(
                    id = "100",
                    playerId = "1",
                    creatureId = "25",
                    creatureSpeciesId = "25",
                    nickname = null,
                    level = 10,
                    abilityInternalName = "static",
                    currentHp = 30,
                    maxHp = 30,
                    statusId = null,
                    storageType = "box",
                    storageBoxId = "200",
                    storageBoxName = "Box 1",
                    storageSlot = 1,
                    partySlot = null,
                    capturedAt = Instant.parse("2026-04-01T00:00:00Z"),
                    captureSessionId = "session-1",
                ),
            )
        val boxesResult =
            listOf(
                CreatureStorageBoxView(
                    id = "200",
                    playerId = "1",
                    name = "Box 1",
                    sortingOrder = 1,
                    capacity = 30,
                ),
            )

        override fun listByPlayerId(playerId: String): List<OwnedCreatureSummaryView> {
            ownedPlayerId = playerId
            return ownedResult
        }

        override fun listBoxesByPlayerId(playerId: String): List<CreatureStorageBoxView> {
            boxesPlayerId = playerId
            return boxesResult
        }
    }
}
