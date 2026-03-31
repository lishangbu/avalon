package io.github.lishangbu.avalon.game.service.player

import java.time.Instant

data class CreatePlayerCommand(
    val userId: String,
    val nickname: String,
    val avatar: String? = null,
)

data class PlayerView(
    val id: String,
    val userId: String,
    val slotNo: Int,
    val nickname: String,
    val avatar: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
