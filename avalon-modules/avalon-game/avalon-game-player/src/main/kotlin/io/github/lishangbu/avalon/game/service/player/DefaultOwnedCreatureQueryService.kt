package io.github.lishangbu.avalon.game.service.player

import io.github.lishangbu.avalon.game.entity.CreatureStorageBox
import io.github.lishangbu.avalon.game.entity.OwnedCreature
import io.github.lishangbu.avalon.game.repository.CreatureStorageBoxRepository
import io.github.lishangbu.avalon.game.repository.OwnedCreatureRepository
import org.springframework.stereotype.Service

@Service
class DefaultOwnedCreatureQueryService(
    private val ownedCreatureRepository: OwnedCreatureRepository,
    private val creatureStorageBoxRepository: CreatureStorageBoxRepository,
) : OwnedCreatureQueryService {
    override fun listByPlayerId(playerId: String): List<OwnedCreatureSummaryView> {
        val parsedPlayerId = playerId.toLongOrNull() ?: error("playerId must be a valid long value.")
        val boxesById =
            creatureStorageBoxRepository
                .findAll()
                .filter { box -> box.playerId == parsedPlayerId }
                .associateBy(CreatureStorageBox::id)
        return ownedCreatureRepository
            .findAll()
            .filter { creature -> creature.playerId == parsedPlayerId }
            .sortedWith(
                compareBy<OwnedCreature> { creature -> creature.storageType }
                    .thenBy { creature -> creature.partySlot ?: Int.MAX_VALUE }
                    .thenBy { creature -> creature.storageBoxId ?: Long.MAX_VALUE }
                    .thenBy { creature -> creature.storageSlot ?: Int.MAX_VALUE }
                    .thenBy { creature -> creature.id },
            ).map { creature ->
                val storageBox = creature.storageBoxId?.let(boxesById::get)
                OwnedCreatureSummaryView(
                    id = creature.id.toString(),
                    playerId = creature.playerId.toString(),
                    creatureId = creature.creatureId.toString(),
                    creatureSpeciesId = creature.creatureSpeciesId.toString(),
                    nickname = creature.nickname,
                    level = creature.level,
                    abilityInternalName = creature.abilityInternalName,
                    currentHp = creature.currentHp,
                    maxHp = creature.maxHp,
                    statusId = creature.statusId,
                    storageType = creature.storageType,
                    storageBoxId = creature.storageBoxId?.toString(),
                    storageBoxName = storageBox?.name,
                    storageSlot = creature.storageSlot,
                    partySlot = creature.partySlot,
                    capturedAt = creature.capturedAt,
                    captureSessionId = creature.captureSessionId,
                )
            }
    }

    override fun listBoxesByPlayerId(playerId: String): List<CreatureStorageBoxView> {
        val parsedPlayerId = playerId.toLongOrNull() ?: error("playerId must be a valid long value.")
        return creatureStorageBoxRepository
            .findAll()
            .filter { box -> box.playerId == parsedPlayerId }
            .sortedWith(compareBy<CreatureStorageBox> { box -> box.sortingOrder }.thenBy { box -> box.id })
            .map { box ->
                CreatureStorageBoxView(
                    id = box.id.toString(),
                    playerId = box.playerId.toString(),
                    name = box.name,
                    sortingOrder = box.sortingOrder,
                    capacity = box.capacity,
                )
            }
    }
}
