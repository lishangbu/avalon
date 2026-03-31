package io.github.lishangbu.avalon.game.battle.engine.unit

import io.github.lishangbu.avalon.game.battle.engine.model.UnitState
import io.github.lishangbu.avalon.game.calculator.growthrate.GrowthRateCalculatorFactory
import io.github.lishangbu.avalon.game.calculator.stat.StatCalculatorFactory

data class CreatureAbilityOptionRecord(
    val internalName: String,
    val slot: Int,
    val hidden: Boolean,
)

data class CreatureUnitImportRecord(
    val id: Long,
    val speciesId: Long?,
    val internalName: String,
    val name: String,
    val weight: Int?,
    val growthRateInternalName: String?,
    val captureRate: Int?,
    val typeIds: List<String>,
    val baseStats: Map<String, Int>,
    val abilityOptions: List<CreatureAbilityOptionRecord>,
)

data class NatureImportRecord(
    val id: Long?,
    val internalName: String,
    val increasedStatInternalName: String?,
    val decreasedStatInternalName: String?,
)

data class BattleMoveSlotInput(
    val moveId: String,
    val currentPp: Int? = null,
)

data class BattleUnitAssemblyRequest(
    val unitId: String,
    val level: Int,
    val abilityInternalName: String? = null,
    val itemId: String? = null,
    val moves: List<BattleMoveSlotInput> = emptyList(),
    val ivs: Map<String, Int> = emptyMap(),
    val evs: Map<String, Int> = emptyMap(),
    val currentHp: Int? = null,
    val statusId: String? = null,
    val volatileIds: Set<String> = emptySet(),
    val conditionIds: Set<String> = emptySet(),
    val boosts: Map<String, Int> = emptyMap(),
    val flags: Map<String, String> = emptyMap(),
    val forceSwitchRequested: Boolean = false,
)

data class BattleUnitAssemblyResult(
    val unit: UnitState,
    val creatureId: Long,
    val creatureInternalName: String,
    val creatureName: String,
    val level: Int,
    val requiredExperience: Int,
    val calculatedStats: Map<String, Int>,
)

object BattleUnitAssembler {
    private const val DEFAULT_IV: Int = 31
    private const val DEFAULT_EV: Int = 0
    private const val NEUTRAL_NATURE: Int = 100
    private const val INCREASED_NATURE: Int = 110
    private const val DECREASED_NATURE: Int = 90

    fun assemble(
        request: BattleUnitAssemblyRequest,
        creature: CreatureUnitImportRecord,
        nature: NatureImportRecord?,
        movePpDefaults: Map<String, Int>,
        statCalculatorFactory: StatCalculatorFactory,
        growthRateCalculatorFactory: GrowthRateCalculatorFactory,
    ): BattleUnitAssemblyResult {
        require(request.level in 1..100) { "Battle unit level must be between 1 and 100." }

        val calculatedStats =
            creature.baseStats.mapValues { (statInternalName, baseStat) ->
                statCalculatorFactory.calculateStat(
                    internalName = statInternalName,
                    base = baseStat,
                    dv = request.ivs[statInternalName] ?: DEFAULT_IV,
                    stateExp = request.evs[statInternalName] ?: DEFAULT_EV,
                    level = request.level,
                    nature = natureModifier(statInternalName, nature),
                )
            }
        val maxHp = (calculatedStats["hp"] ?: 0).coerceAtLeast(1)
        val currentHp = (request.currentHp ?: maxHp).coerceIn(0, maxHp)
        val selectedAbilityId = selectAbilityId(request.abilityInternalName, creature.abilityOptions)
        val requiredExperience =
            growthRateCalculatorFactory.calculateGrowthRate(
                internalName = creature.growthRateInternalName.orEmpty(),
                level = request.level,
            )
        val metadataFlags =
            buildMap {
                put("level", request.level.toString())
                put("creatureId", creature.id.toString())
                creature.speciesId?.let { speciesId -> put("creatureSpeciesId", speciesId.toString()) }
                put("creatureInternalName", creature.internalName)
                put("creatureName", creature.name)
                creature.weight?.let { weight -> put("weight", weight.toString()) }
                creature.captureRate?.let { captureRate -> put("captureRate", captureRate.toString()) }
                put("requiredExperience", requiredExperience.toString())
                put("nature", nature?.internalName ?: "neutral")
                nature?.id?.let { natureId -> put("natureId", natureId.toString()) }
                request.ivs.forEach { (stat, value) -> put("iv.$stat", value.toString()) }
                request.evs.forEach { (stat, value) -> put("ev.$stat", value.toString()) }
            }

        return BattleUnitAssemblyResult(
            unit =
                UnitState(
                    id = request.unitId,
                    currentHp = currentHp,
                    maxHp = maxHp,
                    statusId = request.statusId,
                    abilityId = selectedAbilityId,
                    itemId = request.itemId,
                    typeIds = creature.typeIds.toSet(),
                    volatileIds = request.volatileIds,
                    conditionIds = request.conditionIds,
                    boosts = request.boosts,
                    stats = calculatedStats,
                    movePp =
                        request.moves.associate { slot ->
                            val defaultPp = movePpDefaults[slot.moveId] ?: 0
                            slot.moveId to (slot.currentPp ?: defaultPp)
                        },
                    flags = request.flags + metadataFlags,
                    forceSwitchRequested = request.forceSwitchRequested,
                ),
            creatureId = creature.id,
            creatureInternalName = creature.internalName,
            creatureName = creature.name,
            level = request.level,
            requiredExperience = requiredExperience,
            calculatedStats = calculatedStats,
        )
    }

    private fun natureModifier(
        statInternalName: String,
        nature: NatureImportRecord?,
    ): Int =
        when (statInternalName) {
            nature?.increasedStatInternalName -> INCREASED_NATURE
            nature?.decreasedStatInternalName -> DECREASED_NATURE
            else -> NEUTRAL_NATURE
        }

    private fun selectAbilityId(
        requestedAbilityInternalName: String?,
        abilityOptions: List<CreatureAbilityOptionRecord>,
    ): String? {
        if (requestedAbilityInternalName != null) {
            return requireNotNull(
                abilityOptions.firstOrNull { option -> option.internalName == requestedAbilityInternalName }?.internalName,
            ) {
                "Creature does not provide ability '$requestedAbilityInternalName'."
            }
        }

        return abilityOptions
            .sortedWith(compareBy<CreatureAbilityOptionRecord> { option -> option.hidden }.thenBy { option -> option.slot })
            .firstOrNull()
            ?.internalName
    }
}
