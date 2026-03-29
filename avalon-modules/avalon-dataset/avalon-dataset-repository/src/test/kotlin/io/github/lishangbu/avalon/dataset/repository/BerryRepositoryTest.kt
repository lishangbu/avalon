package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.Berry
import io.github.lishangbu.avalon.dataset.entity.dto.BerrySpecification
import jakarta.annotation.Resource
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Transactional

@Transactional
class BerryRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var berryRepository: BerryRepository

    @Test
    fun shouldQueryPageAndCrudBerry() {
        val condition = BerrySpecification(internalName = "cheri")

        val results = berryRepository.listViews(condition)
        val page = berryRepository.pageViews(condition, PageRequest.of(0, 10))

        assertFalse(results.isEmpty())
        assertEquals("1", results.first().id)
        assertEquals("cheri", results.first().internalName)
        assertTrue(page.totalRowCount >= 1)
        assertFalse(page.rows.isEmpty())

        val existing = requireNotNull(berryRepository.findNullable(1L))
        berryRepository.save(Berry(existing) { name = "更新后的树果" }, SaveMode.UPSERT)

        val updated = requireNotNull(berryRepository.loadViewById(1L))
        assertEquals("更新后的树果", updated.name)

        berryRepository.deleteById(1L)
        assertNull(berryRepository.loadViewById(1L))
    }

    private fun assertTrue(value: Boolean) {
        assertEquals(true, value)
    }
}
