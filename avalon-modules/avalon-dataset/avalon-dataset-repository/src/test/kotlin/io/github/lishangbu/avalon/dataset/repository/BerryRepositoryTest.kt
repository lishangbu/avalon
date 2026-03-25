package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.Berry
import jakarta.annotation.Resource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Example
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Transactional

@Transactional
class BerryRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var berryRepository: BerryRepository

    @Test
    fun shouldQueryPageAndCrudBerry() {
        val condition =
            Berry {
                internalName = "cheri"
            }

        val results = berryRepository.findAll(Example.of(condition))
        val page = berryRepository.findAll(Example.of(condition), PageRequest.of(0, 10))

        assertFalse(results.isEmpty())
        assertEquals(1L, results.first().id)
        assertEquals("cheri", results.first().internalName)
        assertTrue(page.totalRowCount >= 1)
        assertFalse(page.rows.isEmpty())

        val existing = requireNotNull(berryRepository.findById(1L))
        berryRepository.saveAndFlush(Berry(existing) { name = "更新后的树果" })

        val updated = requireNotNull(berryRepository.findById(1L))
        assertEquals("更新后的树果", updated.name)

        berryRepository.deleteById(1L)
        berryRepository.flush()
        assertNull(berryRepository.findById(1L))
    }

    private fun assertTrue(value: Boolean) {
        assertEquals(true, value)
    }
}
