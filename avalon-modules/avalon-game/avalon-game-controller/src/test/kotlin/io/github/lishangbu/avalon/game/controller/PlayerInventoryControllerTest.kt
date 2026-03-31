package io.github.lishangbu.avalon.game.controller

import io.github.lishangbu.avalon.game.service.player.GrantInventoryItemCommand
import io.github.lishangbu.avalon.game.service.player.PlayerInventoryItemView
import io.github.lishangbu.avalon.game.service.player.PlayerInventoryManagementService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class PlayerInventoryControllerTest {
    @Test
    fun listItems_delegatesToService() {
        val service = FakePlayerInventoryManagementService()
        val controller = PlayerInventoryController(service)

        val result = controller.listItems("1")

        assertSame(service.listResult, result)
        assertEquals("1", service.listPlayerId)
    }

    @Test
    fun grantItem_delegatesToService() {
        val service = FakePlayerInventoryManagementService()
        val controller = PlayerInventoryController(service)
        val command = GrantInventoryItemCommand(playerId = "1", itemInternalName = "great-ball", quantity = 5)

        val result = controller.grantItem(command)

        assertSame(service.grantResult, result)
        assertSame(command, service.grantCommand)
    }

    private class FakePlayerInventoryManagementService : PlayerInventoryManagementService {
        var listPlayerId: String? = null
        var grantCommand: GrantInventoryItemCommand? = null

        val listResult =
            listOf(
                PlayerInventoryItemView(
                    playerId = "1",
                    itemId = "2",
                    itemInternalName = "great-ball",
                    itemName = "Great Ball",
                    quantity = 10,
                ),
            )
        val grantResult =
            PlayerInventoryItemView(
                playerId = "1",
                itemId = "2",
                itemInternalName = "great-ball",
                itemName = "Great Ball",
                quantity = 15,
            )

        override fun listByPlayerId(playerId: String): List<PlayerInventoryItemView> {
            listPlayerId = playerId
            return listResult
        }

        override fun grant(command: GrantInventoryItemCommand): PlayerInventoryItemView {
            grantCommand = command
            return grantResult
        }
    }
}
