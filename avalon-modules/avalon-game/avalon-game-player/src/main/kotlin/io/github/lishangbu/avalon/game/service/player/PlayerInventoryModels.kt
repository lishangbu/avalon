package io.github.lishangbu.avalon.game.service.player

data class GrantInventoryItemCommand(
    val playerId: String,
    val itemInternalName: String,
    val quantity: Int,
)

data class PlayerInventoryItemView(
    val playerId: String,
    val itemId: String,
    val itemInternalName: String,
    val itemName: String,
    val quantity: Int,
)
