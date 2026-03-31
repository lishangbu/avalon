package io.github.lishangbu.avalon.game.service.capture

import io.github.lishangbu.avalon.game.entity.PlayerInventoryItem
import io.github.lishangbu.avalon.game.repository.PlayerInventoryItemRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
open class DefaultPlayerInventoryService(
    private val playerInventoryItemRepository: PlayerInventoryItemRepository,
) {
    open fun ensureAvailable(
        playerId: Long,
        itemId: Long,
        count: Int = 1,
    ) {
        val existing = requireInventoryItem(playerId, itemId)
        require(existing.quantity >= count) {
            "Item '$itemId' quantity is insufficient for player '$playerId'."
        }
    }

    @Transactional(rollbackFor = [Exception::class])
    open fun consume(
        playerId: Long,
        itemId: Long,
        count: Int = 1,
    ) {
        val existing = requireInventoryItem(playerId, itemId)
        require(existing.quantity >= count) {
            "Item '$itemId' quantity is insufficient for player '$playerId'."
        }
        playerInventoryItemRepository.save(
            PlayerInventoryItem(existing) {
                quantity = existing.quantity - count
                updatedAt = Instant.now()
            },
        )
    }

    private fun requireInventoryItem(
        playerId: Long,
        itemId: Long,
    ): PlayerInventoryItem =
        requireNotNull(
            playerInventoryItemRepository
                .findAll()
                .firstOrNull { inventoryItem ->
                    inventoryItem.playerId == playerId && inventoryItem.itemId == itemId
                },
        ) {
            "Item '$itemId' is not available for player '$playerId'."
        }
}
