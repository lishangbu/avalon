package io.github.lishangbu.avalon.game.controller

import io.github.lishangbu.avalon.game.service.player.CreatePlayerCommand
import io.github.lishangbu.avalon.game.service.player.PlayerManagementService
import io.github.lishangbu.avalon.game.service.player.PlayerQueryService
import io.github.lishangbu.avalon.game.service.player.PlayerView
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.time.Instant

class PlayerControllerTest {
    @Test
    fun listPlayers_delegatesToService() {
        val queryService = FakePlayerQueryService()
        val managementService = FakePlayerManagementService()
        val controller = PlayerController(queryService, managementService)

        val result = controller.listPlayers("1")

        assertSame(queryService.result, result)
        assertEquals("1", queryService.userId)
    }

    @Test
    fun createPlayer_delegatesToService() {
        val queryService = FakePlayerQueryService()
        val managementService = FakePlayerManagementService()
        val controller = PlayerController(queryService, managementService)
        val command = CreatePlayerCommand(userId = "1", nickname = "Red", avatar = null)

        val result = controller.createPlayer(command)

        assertSame(managementService.result, result)
        assertSame(command, managementService.command)
    }

    private class FakePlayerQueryService : PlayerQueryService {
        var userId: String? = null

        val result =
            listOf(
                PlayerView(
                    id = "10",
                    userId = "1",
                    slotNo = 1,
                    nickname = "Red",
                    avatar = null,
                    createdAt = Instant.parse("2026-04-02T00:00:00Z"),
                    updatedAt = Instant.parse("2026-04-02T00:00:00Z"),
                ),
            )

        override fun listByUserId(userId: String): List<PlayerView> {
            this.userId = userId
            return result
        }
    }

    private class FakePlayerManagementService : PlayerManagementService {
        var command: CreatePlayerCommand? = null

        val result =
            PlayerView(
                id = "10",
                userId = "1",
                slotNo = 1,
                nickname = "Red",
                avatar = null,
                createdAt = Instant.parse("2026-04-02T00:00:00Z"),
                updatedAt = Instant.parse("2026-04-02T00:00:00Z"),
            )

        override fun create(command: CreatePlayerCommand): PlayerView {
            this.command = command
            return result
        }
    }
}
