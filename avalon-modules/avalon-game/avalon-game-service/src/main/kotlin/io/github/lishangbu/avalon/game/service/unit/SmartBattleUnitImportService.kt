package io.github.lishangbu.avalon.game.service.unit

import io.github.lishangbu.avalon.dataset.entity.CreatureAbility
import io.github.lishangbu.avalon.dataset.entity.CreatureElement
import io.github.lishangbu.avalon.dataset.entity.CreatureStat
import io.github.lishangbu.avalon.dataset.entity.Move
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureSpeciesView
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureView
import io.github.lishangbu.avalon.dataset.entity.dto.MoveSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.MoveView
import io.github.lishangbu.avalon.dataset.entity.dto.NatureSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.NatureView
import io.github.lishangbu.avalon.dataset.repository.AbilityRepository
import io.github.lishangbu.avalon.dataset.repository.CreatureAbilityRepository
import io.github.lishangbu.avalon.dataset.repository.CreatureElementRepository
import io.github.lishangbu.avalon.dataset.repository.CreatureRepository
import io.github.lishangbu.avalon.dataset.repository.CreatureSpeciesRepository
import io.github.lishangbu.avalon.dataset.repository.CreatureStatRepository
import io.github.lishangbu.avalon.dataset.repository.MoveRepository
import io.github.lishangbu.avalon.dataset.repository.NatureRepository
import io.github.lishangbu.avalon.dataset.repository.StatRepository
import io.github.lishangbu.avalon.dataset.repository.TypeRepository
import io.github.lishangbu.avalon.game.battle.engine.unit.BattleMoveSlotInput
import io.github.lishangbu.avalon.game.battle.engine.unit.BattleUnitAssembler
import io.github.lishangbu.avalon.game.battle.engine.unit.BattleUnitAssemblyRequest
import io.github.lishangbu.avalon.game.battle.engine.unit.CreatureAbilityOptionRecord
import io.github.lishangbu.avalon.game.battle.engine.unit.CreatureUnitImportRecord
import io.github.lishangbu.avalon.game.battle.engine.unit.NatureImportRecord
import io.github.lishangbu.avalon.game.calculator.growthrate.GrowthRateCalculatorFactory
import io.github.lishangbu.avalon.game.calculator.stat.StatCalculatorFactory
import org.springframework.stereotype.Service

/**
 * 基于真实数据的战斗单位智能导入服务。
 */
@Service
class SmartBattleUnitImportService(
    private val creatureRepository: CreatureRepository,
    private val creatureSpeciesRepository: CreatureSpeciesRepository,
    private val creatureElementRepository: CreatureElementRepository,
    private val creatureStatRepository: CreatureStatRepository,
    private val creatureAbilityRepository: CreatureAbilityRepository,
    private val typeRepository: TypeRepository,
    private val statRepository: StatRepository,
    private val abilityRepository: AbilityRepository,
    private val natureRepository: NatureRepository,
    private val moveRepository: MoveRepository,
    private val statCalculatorFactory: StatCalculatorFactory,
    private val growthRateCalculatorFactory: GrowthRateCalculatorFactory,
) : BattleUnitImporter {
    override fun importUnit(request: BattleUnitImportRequest): BattleUnitImportResult {
        val creature = loadCreature(request)
        val nature = loadNature(request)
        val movePpDefaults = loadMovePpDefaults(request.moves)
        val assembled =
            BattleUnitAssembler.assemble(
                request =
                    BattleUnitAssemblyRequest(
                        unitId = request.unitId,
                        level = request.level,
                        abilityInternalName = request.abilityInternalName,
                        itemId = request.itemId,
                        moves = request.moves.map { move -> BattleMoveSlotInput(move.moveId, move.currentPp) },
                        ivs = request.ivs,
                        evs = request.evs,
                        currentHp = request.currentHp,
                        statusId = request.statusId,
                        volatileIds = request.volatileIds,
                        conditionIds = request.conditionIds,
                        boosts = request.boosts,
                        flags = request.flags,
                        forceSwitchRequested = request.forceSwitchRequested,
                    ),
                creature = creature,
                nature = nature,
                movePpDefaults = movePpDefaults,
                statCalculatorFactory = statCalculatorFactory,
                growthRateCalculatorFactory = growthRateCalculatorFactory,
            )

        return BattleUnitImportResult(
            unit = assembled.unit,
            creatureId = assembled.creatureId,
            creatureInternalName = assembled.creatureInternalName,
            creatureName = assembled.creatureName,
            level = assembled.level,
            requiredExperience = assembled.requiredExperience,
            calculatedStats = assembled.calculatedStats,
        )
    }

    private fun loadCreature(request: BattleUnitImportRequest): CreatureUnitImportRecord {
        val creature =
            when {
                request.creatureId != null -> {
                    creatureRepository.loadViewById(request.creatureId)
                }

                request.creatureInternalName != null -> {
                    creatureRepository
                        .listViews(CreatureSpecification(internalName = request.creatureInternalName))
                        .firstOrNull { view -> view.internalName == request.creatureInternalName }
                }

                else -> {
                    error("Either creatureId or creatureInternalName must be provided.")
                }
            }
                ?: error("Creature was not found for request '${request.unitId}'.")

        val creatureId = creature.id.toLong()
        val species = loadCreatureSpecies(creature)
        val typeRows = loadCreatureElements(creatureId)
        val statRows = loadCreatureStats(creatureId)
        val abilityRows = loadCreatureAbilities(creatureId)

        return CreatureUnitImportRecord(
            id = creatureId,
            speciesId = species?.id?.toLong(),
            internalName = requireNotNull(creature.internalName) { "Creature internalName must not be null." },
            name = creature.name ?: requireNotNull(creature.internalName),
            weight = creature.weight,
            growthRateInternalName = species?.growthRate?.internalName,
            captureRate = species?.captureRate,
            typeIds = typeRows.mapNotNull { type -> type.internalName },
            baseStats = statRows.associate { stat -> stat.internalName to stat.baseStat },
            abilityOptions =
                abilityRows.map { ability ->
                    CreatureAbilityOptionRecord(
                        internalName = ability.internalName,
                        slot = ability.slot,
                        hidden = ability.hidden,
                    )
                },
        )
    }

    private fun loadCreatureSpecies(creature: CreatureView): CreatureSpeciesView? {
        val speciesId = creature.creatureSpecies?.id?.toLong() ?: return null
        return creatureSpeciesRepository.loadViewById(speciesId)
    }

    private fun loadCreatureElements(creatureId: Long): List<io.github.lishangbu.avalon.dataset.entity.Type> {
        val rows = creatureElementRepository.findAll().filter { row -> row.id.creatureId == creatureId }
        val slots = rows.associate { row -> row.id.typeId to (row.slot ?: Int.MAX_VALUE) }
        return typeRepository
            .findAllById(slots.keys)
            .sortedBy { type -> slots[type.id] ?: Int.MAX_VALUE }
    }

    private fun loadCreatureStats(creatureId: Long): List<CreatureStatResolved> {
        val rows = creatureStatRepository.findAll().filter { row -> row.id.creatureId == creatureId }
        val statsById = statRepository.findAllById(rows.map { row -> row.id.statId }.toSet()).associateBy { stat -> stat.id }
        return rows.mapNotNull { row ->
            val stat = statsById[row.id.statId] ?: return@mapNotNull null
            val internalName = stat.internalName ?: return@mapNotNull null
            CreatureStatResolved(
                internalName = internalName,
                baseStat = row.baseStat ?: 0,
            )
        }
    }

    private fun loadCreatureAbilities(creatureId: Long): List<CreatureAbilityResolved> {
        val rows = creatureAbilityRepository.findAll().filter { row -> row.id.creatureId == creatureId }
        val abilitiesById = abilityRepository.findAllById(rows.map { row -> row.id.abilityId }.toSet()).associateBy { ability -> ability.id }
        return rows
            .mapNotNull { row ->
                val ability = abilitiesById[row.id.abilityId] ?: return@mapNotNull null
                val internalName = ability.internalName ?: return@mapNotNull null
                CreatureAbilityResolved(
                    internalName = internalName,
                    slot = row.slot ?: Int.MAX_VALUE,
                    hidden = row.hidden == true,
                )
            }.sortedWith(compareBy<CreatureAbilityResolved> { ability -> ability.hidden }.thenBy { ability -> ability.slot })
    }

    private fun loadNature(request: BattleUnitImportRequest): NatureImportRecord? {
        val nature =
            when {
                request.natureId != null -> {
                    natureRepository.loadViewById(request.natureId)
                }

                request.natureInternalName != null -> {
                    natureRepository
                        .listViews(NatureSpecification(internalName = request.natureInternalName))
                        .firstOrNull { view -> view.internalName == request.natureInternalName }
                }

                else -> {
                    null
                }
            }
                ?: return null

        return NatureImportRecord(
            id = nature.id.toLong(),
            internalName = requireNotNull(nature.internalName) { "Nature internalName must not be null." },
            increasedStatInternalName = nature.increasedStat?.internalName,
            decreasedStatInternalName = nature.decreasedStat?.internalName,
        )
    }

    private fun loadMovePpDefaults(moves: List<BattleMoveImportRequest>): Map<String, Int> =
        moves.associate { move ->
            move.moveId to (loadMove(move.moveId)?.pp ?: 0)
        }

    private fun loadMove(internalName: String): MoveView? =
        moveRepository
            .listViews(MoveSpecification(internalName = internalName))
            .firstOrNull { move -> move.internalName == internalName }

    private data class CreatureStatResolved(
        val internalName: String,
        val baseStat: Int,
    )

    private data class CreatureAbilityResolved(
        val internalName: String,
        val slot: Int,
        val hidden: Boolean,
    )
}
