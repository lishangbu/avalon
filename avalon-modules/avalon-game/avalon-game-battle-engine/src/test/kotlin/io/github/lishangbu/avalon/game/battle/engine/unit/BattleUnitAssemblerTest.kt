package io.github.lishangbu.avalon.game.battle.engine.unit

import io.github.lishangbu.avalon.game.calculator.growthrate.FastGrowthRateCalculator
import io.github.lishangbu.avalon.game.calculator.growthrate.FastThenVerySlowGrowthRateCalculator
import io.github.lishangbu.avalon.game.calculator.growthrate.GrowthRateCalculatorFactory
import io.github.lishangbu.avalon.game.calculator.growthrate.MediumGrowthRateCalculator
import io.github.lishangbu.avalon.game.calculator.growthrate.MediumSlowGrowthRateCalculator
import io.github.lishangbu.avalon.game.calculator.growthrate.SlowGrowthRateCalculator
import io.github.lishangbu.avalon.game.calculator.growthrate.SlowThenVeryFastGrowthRateCalculator
import io.github.lishangbu.avalon.game.calculator.stat.HpStatCalculator
import io.github.lishangbu.avalon.game.calculator.stat.NonHpStatCalculator
import io.github.lishangbu.avalon.game.calculator.stat.StatCalculatorFactory
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BattleUnitAssemblerTest {
    private val statCalculatorFactory = StatCalculatorFactory(listOf(HpStatCalculator(), NonHpStatCalculator()))
    private val growthRateCalculatorFactory =
        GrowthRateCalculatorFactory(
            listOf(
                SlowGrowthRateCalculator(),
                MediumGrowthRateCalculator(),
                FastGrowthRateCalculator(),
                MediumSlowGrowthRateCalculator(),
                SlowThenVeryFastGrowthRateCalculator(),
                FastThenVerySlowGrowthRateCalculator(),
            ),
        )

    @Test
    fun shouldAssembleBattleUnitFromRealisticPokemonData() {
        val result =
            BattleUnitAssembler.assemble(
                request =
                    BattleUnitAssemblyRequest(
                        unitId = "unit-bulbasaur",
                        level = 50,
                        abilityInternalName = null,
                        itemId = "sitrus-berry",
                        moves =
                            listOf(
                                BattleMoveSlotInput("tackle"),
                                BattleMoveSlotInput("growl", currentPp = 20),
                            ),
                        ivs = mapOf("hp" to 31, "attack" to 31, "defense" to 31, "special-attack" to 31, "special-defense" to 31, "speed" to 31),
                        evs = mapOf("hp" to 252, "defense" to 252, "speed" to 4),
                    ),
                creature =
                    CreatureUnitImportRecord(
                        id = 1L,
                        speciesId = 1L,
                        internalName = "bulbasaur",
                        name = "Bulbasaur",
                        weight = 69,
                        growthRateInternalName = "medium-slow",
                        captureRate = 45,
                        typeIds = listOf("grass", "poison"),
                        baseStats =
                            mapOf(
                                "hp" to 45,
                                "attack" to 49,
                                "defense" to 49,
                                "special-attack" to 65,
                                "special-defense" to 65,
                                "speed" to 45,
                            ),
                        abilityOptions =
                            listOf(
                                CreatureAbilityOptionRecord("overgrow", slot = 1, hidden = false),
                                CreatureAbilityOptionRecord("chlorophyll", slot = 3, hidden = true),
                            ),
                    ),
                nature =
                    NatureImportRecord(
                        id = 2L,
                        internalName = "bold",
                        increasedStatInternalName = "defense",
                        decreasedStatInternalName = "attack",
                    ),
                movePpDefaults = mapOf("tackle" to 35, "growl" to 40),
                statCalculatorFactory = statCalculatorFactory,
                growthRateCalculatorFactory = growthRateCalculatorFactory,
            )

        assertEquals("overgrow", result.unit.abilityId)
        assertEquals(setOf("grass", "poison"), result.unit.typeIds)
        assertEquals(152, result.unit.maxHp)
        assertEquals(62, result.unit.stats["attack"])
        assertEquals(111, result.unit.stats["defense"])
        assertEquals(85, result.unit.stats["special-attack"])
        assertEquals(66, result.unit.stats["speed"])
        assertEquals(117360, result.requiredExperience)
        assertEquals(35, result.unit.movePp["tackle"])
        assertEquals(20, result.unit.movePp["growl"])
    }
}
