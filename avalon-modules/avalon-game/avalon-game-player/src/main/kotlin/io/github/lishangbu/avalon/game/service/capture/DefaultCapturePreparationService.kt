package io.github.lishangbu.avalon.game.service.capture

import io.github.lishangbu.avalon.dataset.entity.dto.CreatureSpeciesView
import io.github.lishangbu.avalon.dataset.entity.dto.ItemSpecification
import io.github.lishangbu.avalon.dataset.repository.CreatureSpeciesRepository
import io.github.lishangbu.avalon.dataset.repository.ItemRepository
import io.github.lishangbu.avalon.game.battle.engine.capture.CaptureContext
import io.github.lishangbu.avalon.game.battle.engine.capture.CaptureFormulaInput
import io.github.lishangbu.avalon.game.battle.engine.model.UnitState
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleRuntimeSnapshot
import io.github.lishangbu.avalon.game.battle.engine.service.CaptureFormulaInputResolver
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionCaptureAction
import io.github.lishangbu.avalon.game.repository.OwnedCreatureRepository
import io.github.lishangbu.avalon.game.repository.PlayerRepository
import org.springframework.stereotype.Service

@Service
open class DefaultCapturePreparationService(
    private val itemRepository: ItemRepository,
    private val creatureSpeciesRepository: CreatureSpeciesRepository,
    private val playerRepository: PlayerRepository,
    private val ownedCreatureRepository: OwnedCreatureRepository,
) : CaptureFormulaInputResolver {
    open fun prepare(
        sessionId: String,
        snapshot: BattleRuntimeSnapshot,
        command: CaptureCommand,
    ): PreparedCaptureContext {
        val playerId = command.playerId.toLongOrNull() ?: error("playerId must be a valid long value.")
        requireNotNull(playerRepository.findNullable(playerId)) {
            "Player '$playerId' was not found."
        }
        val targetUnit =
            requireNotNull(snapshot.units[command.targetUnitId]) {
                "Target unit '${command.targetUnitId}' was not found."
            }
        val sourceUnit =
            command.sourceUnitId?.let { sourceUnitId ->
                requireNotNull(snapshot.units[sourceUnitId]) {
                    "Source unit '$sourceUnitId' was not found."
                }
            }
        val targetMetadata = readBattleUnitMetadata(targetUnit)
        val species = loadSpecies(targetMetadata.creatureSpeciesId)
        val item = loadItem(command.ballItemId)
        val captureRate = species.captureRate ?: targetMetadata.captureRate ?: 0
        val alreadyCaught =
            ownedCreatureRepository
                .findAll()
                .any { ownedCreature ->
                    ownedCreature.playerId == playerId && ownedCreature.creatureSpeciesId == targetMetadata.creatureSpeciesId
                }

        return PreparedCaptureContext(
            sessionId = sessionId,
            playerId = playerId,
            ballItemId = item.id.toLong(),
            ballItemInternalName = requireNotNull(item.internalName),
            targetUnitId = command.targetUnitId,
            sourceUnitId = command.sourceUnitId,
            snapshot = snapshot,
            targetUnit = targetUnit,
            sourceUnit = sourceUnit,
            targetMetadata = targetMetadata.copy(captureRate = captureRate),
            battleContext =
                CaptureContext(
                    // Environment-sensitive balls can be driven by imported unit flags such as
                    // `capture.isNight`, `capture.isCave`, `capture.isSurfEncounter`, and `capture.isFishingEncounter`.
                    alreadyCaught = alreadyCaught,
                    isFishingEncounter = targetUnit.captureFlag("isFishingEncounter"),
                    isSurfEncounter = targetUnit.captureFlag("isSurfEncounter"),
                    isNight = targetUnit.captureFlag("isNight"),
                    isCave = targetUnit.captureFlag("isCave"),
                    isUltraBeast = targetUnit.captureFlag("isUltraBeast"),
                    targetLevel = targetMetadata.level,
                    targetWeight = targetUnit.captureIntFlag("targetWeight") ?: targetUnit.captureIntFlag("weight"),
                    targetTypes = targetUnit.typeIds,
                ),
        )
    }

    override fun resolve(
        sessionId: String,
        snapshot: BattleRuntimeSnapshot,
        action: BattleSessionCaptureAction,
    ): CaptureFormulaInput {
        val prepared =
            prepare(
                sessionId = sessionId,
                snapshot = snapshot,
                command =
                    CaptureCommand(
                        playerId = action.playerId,
                        ballItemId = action.ballItemId,
                        targetUnitId = action.targetId,
                        sourceUnitId = action.sourceUnitId,
                    ),
            )
        return CaptureFormulaInput(
            currentHp = prepared.targetUnit.currentHp,
            maxHp = prepared.targetUnit.maxHp,
            captureRate = prepared.targetMetadata.captureRate ?: 0,
            statusId = prepared.targetUnit.statusId,
            ballItemInternalName = prepared.ballItemInternalName,
            turn = snapshot.battle.turn,
            battleContext = prepared.battleContext,
        )
    }

    private fun loadItem(internalName: String) =
        itemRepository
            .listViews(ItemSpecification(internalName = internalName))
            .firstOrNull { item -> item.internalName == internalName }
            ?: error("Capture ball '$internalName' was not found.")

    private fun loadSpecies(speciesId: Long): CreatureSpeciesView =
        requireNotNull(creatureSpeciesRepository.loadViewById(speciesId)) {
            "Creature species '$speciesId' was not found."
        }

    private fun readBattleUnitMetadata(unit: UnitState): BattleUnitMetadata =
        BattleUnitMetadata(
            creatureId = requireLong(unit, "creatureId"),
            creatureSpeciesId = requireLong(unit, "creatureSpeciesId"),
            creatureInternalName = requireString(unit, "creatureInternalName"),
            creatureName = requireString(unit, "creatureName"),
            level = requireInt(unit, "level"),
            requiredExperience = unit.flags["requiredExperience"]?.toIntOrNull() ?: 0,
            natureId = unit.flags["natureId"]?.toLongOrNull(),
            captureRate = unit.flags["captureRate"]?.toIntOrNull(),
            ivs = unit.flags.parseStatMap("iv."),
            evs = unit.flags.parseStatMap("ev."),
            calculatedStats = unit.stats,
        )

    private fun requireString(
        unit: UnitState,
        key: String,
    ): String =
        requireNotNull(unit.flags[key]) {
            "Unit '${unit.id}' is missing required metadata '$key'."
        }

    private fun requireInt(
        unit: UnitState,
        key: String,
    ): Int =
        requireString(unit, key).toIntOrNull()
            ?: error("Unit '${unit.id}' metadata '$key' is not a valid int.")

    private fun requireLong(
        unit: UnitState,
        key: String,
    ): Long =
        requireString(unit, key).toLongOrNull()
            ?: error("Unit '${unit.id}' metadata '$key' is not a valid long.")

    private fun Map<String, String>.parseStatMap(prefix: String): Map<String, Int> =
        entries
            .filter { (key, _) -> key.startsWith(prefix) }
            .mapNotNull { (key, value) ->
                value.toIntOrNull()?.let { parsed -> key.removePrefix(prefix) to parsed }
            }.toMap()

    private fun io.github.lishangbu.avalon.game.battle.engine.model.UnitState.captureFlag(key: String): Boolean =
        flags["capture.$key"]?.toBooleanStrictOrNull()
            ?: flags[key]?.toBooleanStrictOrNull()
            ?: false

    private fun io.github.lishangbu.avalon.game.battle.engine.model.UnitState.captureIntFlag(key: String): Int? =
        flags["capture.$key"]?.toIntOrNull()
            ?: flags[key]?.toIntOrNull()
}
