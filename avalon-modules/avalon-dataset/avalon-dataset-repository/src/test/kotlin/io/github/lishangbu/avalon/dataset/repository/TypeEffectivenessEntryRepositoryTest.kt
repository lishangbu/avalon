package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.TypeEffectivenessEntry
import jakarta.annotation.Resource
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Transactional

@Transactional
class TypeEffectivenessEntryRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var typeEffectivenessEntryRepository: TypeEffectivenessEntryRepository

    @Test
    fun shouldQueryPageAndCrudTypeEffectivenessEntry() {
        val results = typeEffectivenessEntryRepository.listByFilter(2L, null, 200)
        val page = typeEffectivenessEntryRepository.pageByFilter(2L, null, 200, PageRequest.of(0, 10))

        assertFalse(results.isEmpty())
        assertFalse(page.rows.isEmpty())

        val existing = results.first()
        val updated =
            typeEffectivenessEntryRepository.save(
                TypeEffectivenessEntry(existing) {
                    multiplierPercent = 150
                },
                SaveMode.UPSERT,
            )

        val foundAfterUpdate =
            typeEffectivenessEntryRepository.listByFilter(
                updated.id.attackingTypeId,
                updated.id.defendingTypeId,
                150,
            )
        assertFalse(foundAfterUpdate.isEmpty())

        typeEffectivenessEntryRepository.deleteById(updated.id)
        val afterDelete =
            typeEffectivenessEntryRepository.listByFilter(
                updated.id.attackingTypeId,
                updated.id.defendingTypeId,
                150,
            )
        assertEquals(0, afterDelete.size)
    }
}
