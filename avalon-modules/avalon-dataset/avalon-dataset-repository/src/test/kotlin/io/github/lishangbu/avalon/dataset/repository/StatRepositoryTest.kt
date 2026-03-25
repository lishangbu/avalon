package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.Stat
import jakarta.annotation.Resource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Example
import org.springframework.transaction.annotation.Transactional

@Transactional
class StatRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var statRepository: StatRepository

    @Test
    fun shouldListAndCrudStat() {
        val condition =
            Stat {
                internalName = "hp"
            }

        val results = statRepository.findAll(Example.of(condition))

        assertFalse(results.isEmpty())
        assertEquals(1L, results.first().id)
        assertEquals("hp", results.first().internalName)

        val saved =
            statRepository.save(
                Stat {
                    id = 990002L
                    internalName = "unit-stat"
                    name = "单元测试能力"
                    gameIndex = 99
                    battleOnly = false
                    moveDamageClassId = 2L
                },
            )

        val inserted = requireNotNull(statRepository.findAll(Example.of(Stat { internalName = "unit-stat" })).firstOrNull())
        assertEquals(saved.id, inserted.id)
        assertEquals("单元测试能力", inserted.name)

        statRepository.save(Stat(inserted) { name = "更新后的能力" })
        val updated = requireNotNull(statRepository.findAll(Example.of(Stat { internalName = "unit-stat" })).firstOrNull())
        assertEquals("更新后的能力", updated.name)

        statRepository.deleteById(saved.id)
        assertTrue(statRepository.findAll(Example.of(Stat { internalName = "unit-stat" })).isEmpty())
    }

    private fun assertTrue(value: Boolean) {
        assertEquals(true, value)
    }
}
