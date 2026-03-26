package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.TypeEffectivenessEntry
import jakarta.annotation.Resource
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
        val results = typeEffectivenessEntryRepository.findAll(2L, null, 200)
        val page = typeEffectivenessEntryRepository.findPage(2L, null, 200, PageRequest.of(0, 10))

        assertFalse(results.isEmpty())
        assertFalse(page.rows.isEmpty())

        val existing = results.first()
        val updated =
            typeEffectivenessEntryRepository.save(
                TypeEffectivenessEntry(existing) {
                    multiplierPercent = 150
                },
            )

        val foundAfterUpdate =
            typeEffectivenessEntryRepository.findAll(
                updated.id.attackingTypeId,
                updated.id.defendingTypeId,
                150,
            )
        assertFalse(foundAfterUpdate.isEmpty())

        typeEffectivenessEntryRepository.deleteById(updated.id)
        val afterDelete =
            typeEffectivenessEntryRepository.findAll(
                updated.id.attackingTypeId,
                updated.id.defendingTypeId,
                150,
            )
        assertEquals(0, afterDelete.size)
    }
}
