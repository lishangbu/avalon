package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.Type
import io.github.lishangbu.avalon.dataset.entity.TypeEffectivenessEntry
import io.github.lishangbu.avalon.dataset.entity.TypeEffectivenessEntryId
import io.github.lishangbu.avalon.dataset.repository.TypeEffectivenessEntryRepository
import io.github.lishangbu.avalon.dataset.repository.TypeRepository
import io.github.lishangbu.avalon.dataset.service.TypeEffectivenessMatrixCellInput
import io.github.lishangbu.avalon.dataset.service.UpsertTypeEffectivenessMatrixCommand
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal

class TypeEffectivenessServiceImplTest {
    @Test
    fun calculateReturnsCombinedMultiplierForDualTypes() {
        val context =
            newContext(
                typeViews = typeViews("fire", "grass", "steel", "water"),
                relationViews =
                    mutableListOf(
                        relation("fire", "grass", "2"),
                        relation("fire", "steel", "2"),
                    ),
            )

        val result = context.service.calculate(" fire ", listOf("grass", "steel"))

        assertEquals("fire", result.attackingType.internalName)
        assertMultiplierEquals("4", result.finalMultiplier)
        assertEquals("complete", result.status)
        assertEquals("super-effective", result.effectiveness)
        assertEquals(listOf("configured", "configured"), result.defendingTypes.map { it.status })
    }

    @Test
    fun calculateReturnsIncompleteWhenRelationIsMissing() {
        val context =
            newContext(
                typeViews = typeViews("fire", "grass", "steel"),
                relationViews = mutableListOf(relation("fire", "grass", "2")),
            )

        val result = context.service.calculate("fire", listOf("grass", "steel"))

        assertNull(result.finalMultiplier)
        assertEquals("incomplete", result.status)
        assertEquals("incomplete", result.effectiveness)
        assertEquals(listOf("2", null), result.defendingTypes.map { it.multiplier?.normalizedString() })
        assertEquals(listOf("configured", "missing"), result.defendingTypes.map { it.status })
    }

    @Test
    fun calculateRejectsDuplicateDefendingTypes() {
        val context = newContext(typeViews = typeViews("fire", "grass"))

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                context.service.calculate("fire", listOf("grass", "grass"))
            }

        assertTrue(exception.message!!.contains("duplicates"))
    }

    @Test
    fun calculatePreservesQuarterMultiplierWithFixedPointArithmetic() {
        val context =
            newContext(
                typeViews = typeViews("fire", "grass", "water"),
                relationViews =
                    mutableListOf(
                        relation("fire", "grass", "0.5"),
                        relation("fire", "water", "0.5"),
                    ),
            )

        val result = context.service.calculate("fire", listOf("grass", "water"))

        assertMultiplierEquals("0.25", result.finalMultiplier)
        assertEquals("not-very-effective", result.effectiveness)
        assertEquals(listOf("0.5", "0.5"), result.defendingTypes.map { it.multiplier?.normalizedString() })
    }

    @Test
    fun getChartReturnsFullMatrixAndCompleteness() {
        val context =
            newContext(
                typeViews = typeViews("fire", "grass", "water"),
                relationViews =
                    mutableListOf(
                        relation("fire", "grass", "2"),
                        relation("fire", "water", "0.5"),
                    ),
            )

        val chart = context.service.getChart()

        assertEquals(3, chart.supportedTypes.size)
        assertEquals(9, chart.completeness.expectedPairs)
        assertEquals(2, chart.completeness.configuredPairs)
        assertEquals(7, chart.completeness.missingPairs)
        val fireRow = chart.rows.first { it.attackingType.internalName == "fire" }
        assertEquals(3, fireRow.cells.size)
        assertMultiplierEquals("2", fireRow.cells.first { it.defendingType.internalName == "grass" }.multiplier)
        assertEquals("missing", fireRow.cells.first { it.defendingType.internalName == "fire" }.status)
    }

    @Test
    fun getChartExcludesBattleOnlyTypesFromMatrix() {
        val context =
            newContext(
                typeViews =
                    listOf(
                        TypeView(id = 1, internalName = "fire"),
                        TypeView(id = 2, internalName = "grass"),
                        TypeView(id = 3, internalName = "stellar", battleOnly = true),
                    ),
                relationViews =
                    mutableListOf(
                        relation("fire", "grass", "2"),
                        relation("fire", "stellar", "2"),
                        relation("stellar", "fire", "0.5"),
                    ),
            )

        val chart = context.service.getChart()

        assertEquals(listOf("fire", "grass"), chart.supportedTypes.map { it.internalName })
        assertEquals(4, chart.completeness.expectedPairs)
        assertEquals(1, chart.completeness.configuredPairs)
        assertEquals(3, chart.completeness.missingPairs)
        assertEquals(listOf("fire", "grass"), chart.rows.map { it.attackingType.internalName })
        assertTrue(chart.rows.flatMap { it.cells }.none { it.defendingType.internalName == "stellar" })
    }

    @Test
    fun upsertMatrixSavesConfiguredCellsDeletesNullCellsAndReturnsLatestChart() {
        val context =
            newContext(
                typeViews = typeViews("fire", "grass", "water"),
                relationViews = mutableListOf(relation("water", "fire", "0.5")),
            )

        val chart =
            context.service.upsertMatrix(
                UpsertTypeEffectivenessMatrixCommand(
                    cells =
                        listOf(
                            TypeEffectivenessMatrixCellInput("fire", "grass", BigDecimal("2")),
                            TypeEffectivenessMatrixCellInput("water", "fire", null),
                        ),
                ),
            )

        assertEquals(1, context.savedRelations.size)
        assertEquals(
            context.idOf("fire"),
            context.savedRelations
                .single()
                .id.attackingTypeId,
        )
        assertEquals(
            context.idOf("grass"),
            context.savedRelations
                .single()
                .id.defendingTypeId,
        )
        assertEquals(200, context.savedRelations.single().multiplierPercent)
        assertEquals(1, context.deletedIds.size)
        assertEquals(context.idOf("water"), context.deletedIds.single().attackingTypeId)
        assertEquals(context.idOf("fire"), context.deletedIds.single().defendingTypeId)
        assertEquals(1, chart.completeness.configuredPairs)
        assertMultiplierEquals(
            "2",
            chart.rows
                .first { it.attackingType.internalName == "fire" }
                .cells
                .first { it.defendingType.internalName == "grass" }
                .multiplier,
        )
    }

    @Test
    fun upsertMatrixRejectsBattleOnlyTypes() {
        val context =
            newContext(
                typeViews =
                    listOf(
                        TypeView(id = 1, internalName = "fire"),
                        TypeView(id = 2, internalName = "stellar", battleOnly = true),
                    ),
            )

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                context.service.upsertMatrix(
                    UpsertTypeEffectivenessMatrixCommand(
                        cells = listOf(TypeEffectivenessMatrixCellInput("fire", "stellar", BigDecimal("2"))),
                    ),
                )
            }

        assertTrue(exception.message!!.contains("Unsupported cells.defendingType: stellar"))
        assertTrue(context.savedRelations.isEmpty())
        assertTrue(context.deletedIds.isEmpty())
    }

    @Test
    fun upsertMatrixRejectsUnsupportedMultiplier() {
        val context = newContext(typeViews = typeViews("fire", "grass"))

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                context.service.upsertMatrix(
                    UpsertTypeEffectivenessMatrixCommand(
                        cells = listOf(TypeEffectivenessMatrixCellInput("fire", "grass", BigDecimal("4"))),
                    ),
                )
            }

        assertTrue(exception.message!!.contains("multiplier"))
        assertTrue(context.savedRelations.isEmpty())
        assertTrue(context.deletedIds.isEmpty())
    }

    private fun newContext(
        typeViews: List<TypeView>,
        relationViews: MutableList<RelationView> = mutableListOf(),
    ): TestContext {
        val typeRepository = mock(TypeRepository::class.java)
        val relationRepository = mock(TypeEffectivenessEntryRepository::class.java)
        val types =
            typeViews.map { view ->
                Type {
                    id = view.id
                    internalName = view.internalName
                    name = view.internalName
                    battleOnly = view.battleOnly
                }
            }
        val idsByInternalName = typeViews.associate { it.internalName to it.id }
        val relations =
            relationViews
                .map { relation -> relation.toEntity(idsByInternalName) }
                .toMutableList()
        val savedRelations = mutableListOf<TypeEffectivenessEntry>()
        val deletedIds = mutableListOf<TypeEffectivenessEntryId>()

        `when`(typeRepository.findAll()).thenReturn(types)
        `when`(relationRepository.listByFilter(isNull(), isNull(), isNull())).thenAnswer { invocation ->
            val attackingTypeId = invocation.getArgument<Long?>(0)
            val defendingTypeId = invocation.getArgument<Long?>(1)
            val multiplierPercent = invocation.getArgument<Int?>(2)
            relations.filter { relation ->
                (attackingTypeId == null || relation.id.attackingTypeId == attackingTypeId) &&
                    (defendingTypeId == null || relation.id.defendingTypeId == defendingTypeId) &&
                    (multiplierPercent == null || relation.multiplierPercent == multiplierPercent)
            }
        }
        `when`(relationRepository.save(any<TypeEffectivenessEntry>(), eq(SaveMode.UPSERT), eq(AssociatedSaveMode.REPLACE), isNull())).thenAnswer { invocation ->
            val entry = invocation.getArgument<TypeEffectivenessEntry>(0)
            savedRelations += entry
            relations.removeIf { existing ->
                existing.id.attackingTypeId == entry.id.attackingTypeId &&
                    existing.id.defendingTypeId == entry.id.defendingTypeId
            }
            relations += entry
            entry
        }
        doAnswer { invocation ->
            val id = invocation.getArgument<TypeEffectivenessEntryId>(0)
            deletedIds += id
            relations.removeIf { existing ->
                existing.id.attackingTypeId == id.attackingTypeId &&
                    existing.id.defendingTypeId == id.defendingTypeId
            }
            null
        }.`when`(relationRepository).deleteById(any<TypeEffectivenessEntryId>())

        return TestContext(
            service = TypeEffectivenessServiceImpl(typeRepository, relationRepository),
            idsByInternalName = idsByInternalName,
            savedRelations = savedRelations,
            deletedIds = deletedIds,
        )
    }

    private fun typeViews(vararg internalNames: String): List<TypeView> =
        internalNames.mapIndexed { index, internalName ->
            TypeView(
                id = (index + 1).toLong(),
                internalName = internalName,
            )
        }

    private fun relation(
        attackingType: String,
        defendingType: String,
        multiplier: String?,
    ): RelationView =
        RelationView(
            attackingType = attackingType,
            defendingType = defendingType,
            multiplier = multiplier?.let(::BigDecimal),
        )

    private data class TestContext(
        val service: TypeEffectivenessServiceImpl,
        val idsByInternalName: Map<String, Long>,
        val savedRelations: MutableList<TypeEffectivenessEntry>,
        val deletedIds: MutableList<TypeEffectivenessEntryId>,
    ) {
        fun idOf(internalName: String): Long = requireNotNull(idsByInternalName[internalName])
    }

    private data class TypeView(
        val id: Long,
        val internalName: String,
        val battleOnly: Boolean = false,
    )

    private data class RelationView(
        val attackingType: String,
        val defendingType: String,
        val multiplier: BigDecimal?,
    )

    private fun RelationView.toEntity(idsByInternalName: Map<String, Long>): TypeEffectivenessEntry =
        TypeEffectivenessEntry {
            id =
                TypeEffectivenessEntryId {
                    attackingTypeId = requireNotNull(idsByInternalName[attackingType])
                    defendingTypeId = requireNotNull(idsByInternalName[defendingType])
                }
            multiplierPercent = TypeEffectivenessMultiplierCodec.encodeEntryMultiplier(this@toEntity.multiplier)
        }

    private fun assertMultiplierEquals(
        expected: String,
        actual: BigDecimal?,
    ) {
        assertNotNull(actual)
        val actualValue = requireNotNull(actual)
        assertEquals(0, BigDecimal(expected).compareTo(actualValue))
    }

    private fun BigDecimal.normalizedString(): String = stripTrailingZeros().toPlainString()
}
