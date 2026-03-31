package io.github.lishangbu.avalon.game.service.battle

import io.github.lishangbu.avalon.dataset.service.TypeEffectivenessChart
import io.github.lishangbu.avalon.dataset.service.TypeEffectivenessResult
import io.github.lishangbu.avalon.dataset.service.TypeEffectivenessService
import io.github.lishangbu.avalon.dataset.service.TypeEffectivenessTypeView
import io.github.lishangbu.avalon.dataset.service.UpsertTypeEffectivenessMatrixCommand
import io.github.lishangbu.avalon.game.battle.engine.model.BattleState
import io.github.lishangbu.avalon.game.battle.engine.model.FieldState
import io.github.lishangbu.avalon.game.battle.engine.model.SideState
import io.github.lishangbu.avalon.game.battle.engine.model.UnitState
import io.github.lishangbu.avalon.game.battle.engine.repository.memory.InMemoryEffectDefinitionRepository
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleRuntimeSnapshot
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionQuery
import io.github.lishangbu.avalon.game.battle.engine.session.target.DefaultBattleSessionAvailableTargetResolver
import io.github.lishangbu.avalon.game.battle.engine.session.target.DefaultBattleSessionTargetModeResolver
import io.github.lishangbu.avalon.game.battle.engine.session.target.DefaultBattleSessionTargetQueryService
import io.github.lishangbu.avalon.game.service.effect.MoveImportRecord
import io.github.lishangbu.avalon.game.service.effect.SmartBattleEffectAssembler
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class DefaultBattleChoiceFactoryTest {
    private val thunderbolt =
        SmartBattleEffectAssembler.fromMove(
            MoveImportRecord(
                internalName = "thunderbolt",
                name = "十万伏特",
                typeInternalName = "electric",
                damageClassInternalName = "special",
                targetInternalName = "selected-pokemon",
                accuracy = 100,
                effectChance = 10,
                pp = 15,
                priority = 0,
                power = 90,
                shortEffect = "May paralyze the target.",
                effect = "Inflicts damage and may paralyze the target.",
                ailmentInternalName = "paralysis",
                ailmentChance = 10,
                healing = null,
                drain = null,
            ),
        )

    private val factory =
        DefaultBattleChoiceFactory(
            effectDefinitionRepository = InMemoryEffectDefinitionRepository(mapOf(thunderbolt.id to thunderbolt)),
            typeEffectivenessService = FakeTypeEffectivenessService(),
            targetQueryService =
                DefaultBattleSessionTargetQueryService(
                    targetModeResolver =
                        DefaultBattleSessionTargetModeResolver(
                            InMemoryEffectDefinitionRepository(mapOf(thunderbolt.id to thunderbolt)),
                        ),
                    availableTargetResolver = DefaultBattleSessionAvailableTargetResolver(),
                ),
        )

    @Test
    fun shouldBuildMoveChoiceFromBattleSnapshot() {
        val session = battleSessionQuery()

        val choice =
            factory.createMoveChoice(
                session = session,
                request =
                    SmartMoveChoiceRequest(
                        attackerId = "attacker",
                        moveId = "thunderbolt",
                        accuracyRoll = 17,
                    ),
            )

        assertEquals("defender", choice.targetId)
        assertEquals(120, choice.speed)
        assertEquals(100, choice.accuracy)
        assertEquals(100, choice.evasion)
        assertEquals(90, choice.basePower)
        assertEquals(148, choice.damage)
        assertEquals(17, choice.attributes["accuracyRoll"])
    }

    @Test
    fun shouldExposeQueryTargetsUsingImportedEffectDefinition() {
        val session = battleSessionQuery()

        val targetQuery = factory.queryTargets(session, "thunderbolt", "attacker")

        assertEquals("thunderbolt", targetQuery.effectId)
        assertEquals(listOf("defender"), targetQuery.availableTargetUnitIds)
        assertFalse(targetQuery.availableTargetUnitIds.isEmpty())
        assertEquals(true, targetQuery.requiresExplicitTarget)
    }

    private fun battleSessionQuery(): BattleSessionQuery =
        BattleSessionQuery(
            snapshot =
                BattleRuntimeSnapshot(
                    battle = BattleState(id = "session-1", formatId = "single", started = true, turn = 1),
                    field = FieldState(),
                    units =
                        mapOf(
                            "attacker" to
                                UnitState(
                                    id = "attacker",
                                    currentHp = 140,
                                    maxHp = 140,
                                    typeIds = setOf("electric"),
                                    stats =
                                        mapOf(
                                            "speed" to 120,
                                            "special-attack" to 120,
                                            "special-defense" to 90,
                                        ),
                                    flags = mapOf("level" to "50"),
                                ),
                            "defender" to
                                UnitState(
                                    id = "defender",
                                    currentHp = 160,
                                    maxHp = 160,
                                    typeIds = setOf("water"),
                                    stats =
                                        mapOf(
                                            "speed" to 80,
                                            "special-attack" to 80,
                                            "special-defense" to 100,
                                        ),
                                    flags = mapOf("level" to "50"),
                                ),
                        ),
                    sides =
                        mapOf(
                            "side-a" to SideState(id = "side-a", unitIds = listOf("attacker"), activeUnitIds = listOf("attacker")),
                            "side-b" to SideState(id = "side-b", unitIds = listOf("defender"), activeUnitIds = listOf("defender")),
                        ),
                ),
            pendingActions = emptyList(),
            choiceStatuses = emptyList(),
            replacementRequests = emptyList(),
            resourceLedger = emptyList(),
            battleLogs = emptyList(),
            eventLogs = emptyList(),
        )

    private class FakeTypeEffectivenessService : TypeEffectivenessService {
        override fun calculate(
            attackingType: String,
            defendingTypes: List<String>,
        ): TypeEffectivenessResult =
            TypeEffectivenessResult(
                attackingType = TypeEffectivenessTypeView(attackingType, attackingType),
                defendingTypes =
                    defendingTypes.map { defendingType ->
                        io.github.lishangbu.avalon.dataset.service.TypeEffectivenessMatchup(
                            defendingType = TypeEffectivenessTypeView(defendingType, defendingType),
                            multiplier = if (attackingType == "electric" && defendingType == "water") BigDecimal("2") else BigDecimal.ONE,
                            status = "configured",
                        )
                    },
                finalMultiplier = if (attackingType == "electric" && defendingTypes == listOf("water")) BigDecimal("2") else BigDecimal.ONE,
                status = "configured",
                effectiveness = "computed",
            )

        override fun getChart(): TypeEffectivenessChart = error("Not needed for test.")

        override fun upsertMatrix(command: UpsertTypeEffectivenessMatrixCommand): TypeEffectivenessChart = error("Not needed for test.")
    }
}
