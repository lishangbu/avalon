package io.github.lishangbu.avalon.game.service.player

import io.github.lishangbu.avalon.dataset.entity.Item
import io.github.lishangbu.avalon.dataset.entity.dto.ItemSpecification
import io.github.lishangbu.avalon.dataset.repository.ItemRepository
import io.github.lishangbu.avalon.game.entity.PlayerInventoryItem
import io.github.lishangbu.avalon.game.repository.PlayerInventoryItemRepository
import io.github.lishangbu.avalon.game.repository.PlayerRepository
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class DefaultPlayerInventoryManagementService(
    private val playerRepository: PlayerRepository,
    private val playerInventoryItemRepository: PlayerInventoryItemRepository,
    private val itemRepository: ItemRepository,
) : PlayerInventoryManagementService {
    override fun listByPlayerId(playerId: String): List<PlayerInventoryItemView> {
        val parsedPlayerId = playerId.toLongOrNull() ?: error("playerId must be a valid long value.")
        val inventoryItems =
            playerInventoryItemRepository
                .findAll()
                .filter { item -> item.playerId == parsedPlayerId }
        if (inventoryItems.isEmpty()) {
            return emptyList()
        }
        val itemsById = itemRepository.findAllById(inventoryItems.map { inventoryItem -> inventoryItem.itemId }.toSet()).associateBy(Item::id)
        return inventoryItems
            .sortedBy { inventoryItem -> inventoryItem.itemId }
            .map { inventoryItem ->
                val item =
                    requireNotNull(itemsById[inventoryItem.itemId]) {
                        "Item '${inventoryItem.itemId}' was not found."
                    }
                PlayerInventoryItemView(
                    playerId = inventoryItem.playerId.toString(),
                    itemId = item.id.toString(),
                    itemInternalName = requireNotNull(item.internalName),
                    itemName = item.name ?: requireNotNull(item.internalName),
                    quantity = inventoryItem.quantity,
                )
            }
    }

    @Transactional(rollbackFor = [Exception::class])
    override fun grant(command: GrantInventoryItemCommand): PlayerInventoryItemView {
        require(command.quantity > 0) { "Grant quantity must be greater than 0." }
        val playerId = command.playerId.toLongOrNull() ?: error("playerId must be a valid long value.")
        requireNotNull(playerRepository.findNullable(playerId)) {
            "Player '$playerId' was not found."
        }
        val itemView =
            itemRepository
                .listViews(ItemSpecification(internalName = command.itemInternalName))
                .firstOrNull { item -> item.internalName == command.itemInternalName }
                ?: error("Item '${command.itemInternalName}' was not found.")
        val itemId = itemView.id.toLong()
        val existing =
            playerInventoryItemRepository
                .findAll()
                .firstOrNull { inventoryItem -> inventoryItem.playerId == playerId && inventoryItem.itemId == itemId }
        val saved =
            if (existing == null) {
                playerInventoryItemRepository.save(
                    PlayerInventoryItem {
                        this.playerId = playerId
                        this.itemId = itemId
                        quantity = command.quantity
                        updatedAt = Instant.now()
                    },
                    SaveMode.INSERT_ONLY,
                )
            } else {
                playerInventoryItemRepository.save(
                    PlayerInventoryItem(existing) {
                        quantity = existing.quantity + command.quantity
                        updatedAt = Instant.now()
                    },
                )
            }

        return PlayerInventoryItemView(
            playerId = saved.playerId.toString(),
            itemId = itemId.toString(),
            itemInternalName = requireNotNull(itemView.internalName),
            itemName = itemView.name ?: requireNotNull(itemView.internalName),
            quantity = saved.quantity,
        )
    }
}
