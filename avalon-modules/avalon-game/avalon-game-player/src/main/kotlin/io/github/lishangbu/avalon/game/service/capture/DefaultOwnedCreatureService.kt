package io.github.lishangbu.avalon.game.service.capture

import io.github.lishangbu.avalon.dataset.entity.dto.MoveSpecification
import io.github.lishangbu.avalon.dataset.repository.MoveRepository
import io.github.lishangbu.avalon.dataset.repository.StatRepository
import io.github.lishangbu.avalon.game.entity.CreatureStorageBox
import io.github.lishangbu.avalon.game.entity.OwnedCreature
import io.github.lishangbu.avalon.game.entity.OwnedCreatureMove
import io.github.lishangbu.avalon.game.entity.OwnedCreatureStat
import io.github.lishangbu.avalon.game.repository.CreatureStorageBoxRepository
import io.github.lishangbu.avalon.game.repository.OwnedCreatureMoveRepository
import io.github.lishangbu.avalon.game.repository.OwnedCreatureRepository
import io.github.lishangbu.avalon.game.repository.OwnedCreatureStatRepository
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
open class DefaultOwnedCreatureService(
    private val creatureStorageBoxRepository: CreatureStorageBoxRepository,
    private val ownedCreatureRepository: OwnedCreatureRepository,
    private val ownedCreatureStatRepository: OwnedCreatureStatRepository,
    private val ownedCreatureMoveRepository: OwnedCreatureMoveRepository,
    private val statRepository: StatRepository,
    private val moveRepository: MoveRepository,
) {
    @Transactional(rollbackFor = [Exception::class])
    open fun capture(context: PreparedCaptureContext): CapturedCreatureSummary {
        val now = Instant.now()
        val box = ensureDefaultBox(context.playerId, now)
        val slot = nextBoxSlot(box.id)
        val metadata = context.targetMetadata
        val ownedCreature =
            ownedCreatureRepository.save(
                OwnedCreature {
                    playerId = context.playerId
                    creatureId = metadata.creatureId
                    creatureSpeciesId = metadata.creatureSpeciesId
                    nickname = null
                    level = metadata.level
                    experience = metadata.requiredExperience
                    natureId = metadata.natureId
                    abilityInternalName = context.targetUnit.abilityId
                    currentHp = context.targetUnit.currentHp
                    maxHp = context.targetUnit.maxHp
                    statusId = context.targetUnit.statusId
                    storageType = STORAGE_TYPE_BOX
                    storageBoxId = box.id
                    storageSlot = slot
                    partySlot = null
                    capturedAt = now
                    captureItemId = context.ballItemId
                    captureSessionId = context.sessionId
                    sourceType = SOURCE_TYPE_CAPTURE
                    createdAt = now
                    updatedAt = now
                },
                SaveMode.INSERT_ONLY,
            )
        saveStats(ownedCreature.id, metadata)
        saveMoves(ownedCreature.id, context)

        return CapturedCreatureSummary(
            ownedCreatureId = ownedCreature.id.toString(),
            creatureId = metadata.creatureId.toString(),
            creatureSpeciesId = metadata.creatureSpeciesId.toString(),
            creatureInternalName = metadata.creatureInternalName,
            creatureName = metadata.creatureName,
        )
    }

    private fun ensureDefaultBox(
        playerId: Long,
        now: Instant,
    ): CreatureStorageBox =
        creatureStorageBoxRepository
            .findAll()
            .filter { box -> box.playerId == playerId }
            .sortedWith(compareBy<CreatureStorageBox> { box -> box.sortingOrder }.thenBy { box -> box.id })
            .firstOrNull()
            ?: creatureStorageBoxRepository.save(
                CreatureStorageBox {
                    this.playerId = playerId
                    name = DEFAULT_BOX_NAME
                    sortingOrder = 1
                    capacity = DEFAULT_BOX_CAPACITY
                    createdAt = now
                    updatedAt = now
                },
                SaveMode.INSERT_ONLY,
            )

    private fun nextBoxSlot(storageBoxId: Long): Int =
        (
            ownedCreatureRepository
                .findAll()
                .filter { creature -> creature.storageBoxId == storageBoxId }
                .mapNotNull { creature -> creature.storageSlot }
                .maxOrNull()
                ?: 0
        ) + 1

    private fun saveStats(
        ownedCreatureId: Long,
        metadata: BattleUnitMetadata,
    ) {
        val statIds = statRepository.findAll().associateBy { stat -> stat.internalName }
        metadata.calculatedStats.keys.union(metadata.ivs.keys).union(metadata.evs.keys).forEach { statInternalName ->
            val statId =
                requireNotNull(statIds[statInternalName]?.id) {
                    "Stat '$statInternalName' was not found."
                }
            ownedCreatureStatRepository.save(
                OwnedCreatureStat {
                    this.ownedCreatureId = ownedCreatureId
                    this.statId = statId
                    iv = metadata.ivs[statInternalName] ?: 31
                    ev = metadata.evs[statInternalName] ?: 0
                    calculatedValue = metadata.calculatedStats[statInternalName] ?: 0
                },
                SaveMode.INSERT_ONLY,
            )
        }
    }

    private fun saveMoves(
        ownedCreatureId: Long,
        context: PreparedCaptureContext,
    ) {
        context.targetUnit.movePp.entries.toList().forEachIndexed { index, (moveInternalName, currentPp) ->
            val moveView =
                moveRepository
                    .listViews(MoveSpecification(internalName = moveInternalName))
                    .firstOrNull { view -> view.internalName == moveInternalName }
                    ?: error("Move '$moveInternalName' was not found.")
            ownedCreatureMoveRepository.save(
                OwnedCreatureMove {
                    this.ownedCreatureId = ownedCreatureId
                    slot = index + 1
                    moveId = moveView.id.toLong()
                    this.currentPp = currentPp
                    maxPp = moveView.pp ?: currentPp
                },
                SaveMode.INSERT_ONLY,
            )
        }
    }

    private companion object {
        const val DEFAULT_BOX_NAME: String = "Box 1"
        const val DEFAULT_BOX_CAPACITY: Int = 30
        const val STORAGE_TYPE_BOX: String = "box"
        const val SOURCE_TYPE_CAPTURE: String = "capture"
    }
}
