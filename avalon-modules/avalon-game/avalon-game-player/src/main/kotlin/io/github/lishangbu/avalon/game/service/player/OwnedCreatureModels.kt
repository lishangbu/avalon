package io.github.lishangbu.avalon.game.service.player

import java.time.Instant

data class CreatureStorageBoxView(
    val id: String,
    val playerId: String,
    val name: String,
    val sortingOrder: Int,
    val capacity: Int,
)

data class OwnedCreatureSummaryView(
    val id: String,
    val playerId: String,
    val creatureId: String,
    val creatureSpeciesId: String,
    val nickname: String?,
    val level: Int,
    val abilityInternalName: String?,
    val currentHp: Int,
    val maxHp: Int,
    val statusId: String?,
    val storageType: String,
    val storageBoxId: String?,
    val storageBoxName: String?,
    val storageSlot: Int?,
    val partySlot: Int?,
    val capturedAt: Instant?,
    val captureSessionId: String?,
)
