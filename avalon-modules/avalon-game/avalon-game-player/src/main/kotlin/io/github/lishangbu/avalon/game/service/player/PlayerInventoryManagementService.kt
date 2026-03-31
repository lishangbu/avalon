package io.github.lishangbu.avalon.game.service.player

interface PlayerInventoryManagementService {
    fun listByPlayerId(playerId: String): List<PlayerInventoryItemView>

    fun grant(command: GrantInventoryItemCommand): PlayerInventoryItemView
}
