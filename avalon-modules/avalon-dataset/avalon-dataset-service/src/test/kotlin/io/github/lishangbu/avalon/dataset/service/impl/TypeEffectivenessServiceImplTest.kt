package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.Type
import io.github.lishangbu.avalon.dataset.entity.TypeEffectivenessEntry
import io.github.lishangbu.avalon.dataset.entity.TypeEffectivenessEntryId
import io.github.lishangbu.avalon.dataset.repository.TypeEffectivenessEntryRepository
import io.github.lishangbu.avalon.dataset.repository.TypeRepository
import io.github.lishangbu.avalon.dataset.service.TypeEffectivenessMatrixCellInput
import io.github.lishangbu.avalon.dataset.service.UpsertTypeEffectivenessMatrixCommand
import org.babyfish.jimmer.Page
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Pageable
import java.math.BigDecimal

class TypeEffectivenessServiceImplTest {
    @Test
    fun calculateReturnsCombinedMultiplierForDualTypes() {
        val typeRepository = FakeTypeRepository(typeViews("fire", "grass", "steel", "water"))
        val relationRepository =
            FakeTypeEffectivenessEntryRepository(
                typeRepository = typeRepository,
                relationViews =
                    mutableListOf(
                        relation("fire", "grass", "2"),
                        relation("fire", "steel", "2"),
                    ),
            )
        val service = newService(typeRepository, relationRepository)

        val result = service.calculate(" fire ", listOf("grass", "steel"))

        assertEquals("fire", result.attackingType.internalName)
        assertMultiplierEquals("4", result.finalMultiplier)
        assertEquals("complete", result.status)
        assertEquals("super-effective", result.effectiveness)
        assertEquals(listOf("configured", "configured"), result.defendingTypes.map { it.status })
    }

    @Test
    fun calculateReturnsIncompleteWhenRelationIsMissing() {
        val typeRepository = FakeTypeRepository(typeViews("fire", "grass", "steel"))
        val relationRepository =
            FakeTypeEffectivenessEntryRepository(
                typeRepository = typeRepository,
                relationViews = mutableListOf(relation("fire", "grass", "2")),
            )
        val service = newService(typeRepository, relationRepository)

        val result = service.calculate("fire", listOf("grass", "steel"))

        assertNull(result.finalMultiplier)
        assertEquals("incomplete", result.status)
        assertEquals("incomplete", result.effectiveness)
        assertEquals(listOf("2", null), result.defendingTypes.map { it.multiplier?.normalizedString() })
        assertEquals(listOf("configured", "missing"), result.defendingTypes.map { it.status })
    }

    @Test
    fun calculateRejectsDuplicateDefendingTypes() {
        val typeRepository = FakeTypeRepository(typeViews("fire", "grass"))
        val service = newService(typeRepository, FakeTypeEffectivenessEntryRepository(typeRepository))

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                service.calculate("fire", listOf("grass", "grass"))
            }

        assertTrue(exception.message!!.contains("duplicates"))
    }

    @Test
    fun calculatePreservesQuarterMultiplierWithFixedPointArithmetic() {
        val typeRepository = FakeTypeRepository(typeViews("fire", "grass", "water"))
        val relationRepository =
            FakeTypeEffectivenessEntryRepository(
                typeRepository = typeRepository,
                relationViews =
                    mutableListOf(
                        relation("fire", "grass", "0.5"),
                        relation("fire", "water", "0.5"),
                    ),
            )
        val service = newService(typeRepository, relationRepository)

        val result = service.calculate("fire", listOf("grass", "water"))

        assertMultiplierEquals("0.25", result.finalMultiplier)
        assertEquals("not-very-effective", result.effectiveness)
        assertEquals(listOf("0.5", "0.5"), result.defendingTypes.map { it.multiplier?.normalizedString() })
    }

    @Test
    fun getChartReturnsFullMatrixAndCompleteness() {
        val typeRepository = FakeTypeRepository(typeViews("fire", "grass", "water"))
        val relationRepository =
            FakeTypeEffectivenessEntryRepository(
                typeRepository = typeRepository,
                relationViews =
                    mutableListOf(
                        relation("fire", "grass", "2"),
                        relation("fire", "water", "0.5"),
                    ),
            )
        val service = newService(typeRepository, relationRepository)

        val chart = service.getChart()

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
    fun upsertMatrixSavesConfiguredCellsDeletesNullCellsAndReturnsLatestChart() {
        val typeRepository = FakeTypeRepository(typeViews("fire", "grass", "water"))
        val relationRepository =
            FakeTypeEffectivenessEntryRepository(
                typeRepository = typeRepository,
                relationViews = mutableListOf(relation("water", "fire", "0.5")),
            )
        val service = newService(typeRepository, relationRepository)

        val chart =
            service.upsertMatrix(
                UpsertTypeEffectivenessMatrixCommand(
                    cells =
                        listOf(
                            TypeEffectivenessMatrixCellInput("fire", "grass", BigDecimal("2")),
                            TypeEffectivenessMatrixCellInput("water", "fire", null),
                        ),
                ),
            )

        assertEquals(1, relationRepository.savedRelations.size)
        assertEquals(
            typeRepository.idOf("fire"),
            relationRepository.savedRelations
                .single()
                .id
                .attackingTypeId,
        )
        assertEquals(
            typeRepository.idOf("grass"),
            relationRepository.savedRelations
                .single()
                .id
                .defendingTypeId,
        )
        assertEquals(200, relationRepository.savedRelations.single().multiplierPercent)
        assertEquals(1, relationRepository.deletedIds.size)
        assertEquals(
            typeRepository.idOf("water"),
            relationRepository.deletedIds.single().attackingTypeId,
        )
        assertEquals(
            typeRepository.idOf("fire"),
            relationRepository.deletedIds.single().defendingTypeId,
        )
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
    fun upsertMatrixRejectsUnsupportedMultiplier() {
        val typeRepository = FakeTypeRepository(typeViews("fire", "grass"))
        val relationRepository = FakeTypeEffectivenessEntryRepository(typeRepository)
        val service = newService(typeRepository, relationRepository)

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                service.upsertMatrix(
                    UpsertTypeEffectivenessMatrixCommand(
                        cells = listOf(TypeEffectivenessMatrixCellInput("fire", "grass", BigDecimal("4"))),
                    ),
                )
            }

        assertTrue(exception.message!!.contains("multiplier"))
        assertTrue(relationRepository.savedRelations.isEmpty())
        assertTrue(relationRepository.deletedIds.isEmpty())
    }

    private fun newService(
        typeRepository: FakeTypeRepository,
        relationRepository: FakeTypeEffectivenessEntryRepository,
    ): TypeEffectivenessServiceImpl =
        TypeEffectivenessServiceImpl(
            typeRepository = typeRepository,
            typeEffectivenessEntryRepository = relationRepository,
        )

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

    private data class TypeView(
        val id: Long,
        val internalName: String,
    )

    private data class RelationView(
        val attackingType: String,
        val defendingType: String,
        val multiplier: BigDecimal?,
    )

    private class FakeTypeRepository(
        private val typeViews: List<TypeView>,
    ) : TypeRepository {
        private val types: List<Type> =
            typeViews.map { view ->
                io.github.lishangbu.avalon.dataset.entity.Type {
                    id = view.id
                    internalName = view.internalName
                    name = view.internalName
                }
            }

        private val idsByInternalName: Map<String, Long> = typeViews.associate { it.internalName to it.id }

        fun idOf(internalName: String): Long = requireNotNull(idsByInternalName[internalName])

        override fun findAll(): List<Type> = types

        override fun findAll(specification: io.github.lishangbu.avalon.dataset.entity.dto.TypeSpecification?): List<Type> = types

        override fun findById(id: Long): Type? = types.firstOrNull { it.id == id }

        override fun save(type: Type): Type = type

        override fun saveAndFlush(type: Type): Type = type

        override fun deleteById(id: Long) = Unit

        override fun flush() = Unit
    }

    private class FakeTypeEffectivenessEntryRepository(
        private val typeRepository: FakeTypeRepository,
        relationViews: MutableList<RelationView> = mutableListOf(),
    ) : TypeEffectivenessEntryRepository {
        private val relations: MutableList<TypeEffectivenessEntry> =
            relationViews
                .map { relation ->
                    relation.toEntity()
                }.toMutableList()

        val savedRelations: MutableList<TypeEffectivenessEntry> = mutableListOf()
        val deletedIds: MutableList<TypeEffectivenessEntryId> = mutableListOf()

        override fun findAll(
            attackingTypeId: Long?,
            defendingTypeId: Long?,
            multiplierPercent: Int?,
        ): List<TypeEffectivenessEntry> =
            relations.filter { relation ->
                (attackingTypeId == null || relation.id.attackingTypeId == attackingTypeId) &&
                    (defendingTypeId == null || relation.id.defendingTypeId == defendingTypeId) &&
                    (multiplierPercent == null || relation.multiplierPercent == multiplierPercent)
            }

        override fun findPage(
            attackingTypeId: Long?,
            defendingTypeId: Long?,
            multiplierPercent: Int?,
            pageable: Pageable,
        ): Page<TypeEffectivenessEntry> = Page(findAll(attackingTypeId, defendingTypeId, multiplierPercent), 1, relations.size.toLong())

        override fun save(typeEffectivenessEntry: TypeEffectivenessEntry): TypeEffectivenessEntry {
            savedRelations += typeEffectivenessEntry
            relations.removeIf { existing ->
                existing.id.attackingTypeId == typeEffectivenessEntry.id.attackingTypeId &&
                    existing.id.defendingTypeId == typeEffectivenessEntry.id.defendingTypeId
            }
            relations += typeEffectivenessEntry
            return typeEffectivenessEntry
        }

        override fun deleteById(id: TypeEffectivenessEntryId) {
            deletedIds += id
            relations.removeIf { existing ->
                existing.id.attackingTypeId == id.attackingTypeId &&
                    existing.id.defendingTypeId == id.defendingTypeId
            }
        }

        private fun RelationView.toEntity(): TypeEffectivenessEntry =
            io.github.lishangbu.avalon.dataset.entity.TypeEffectivenessEntry {
                id =
                    io.github.lishangbu.avalon.dataset.entity.TypeEffectivenessEntryId {
                        attackingTypeId = typeRepository.idOf(attackingType)
                        defendingTypeId = typeRepository.idOf(defendingType)
                    }
                multiplierPercent = TypeEffectivenessMultiplierCodec.encodeEntryMultiplier(this@toEntity.multiplier)
            }
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
