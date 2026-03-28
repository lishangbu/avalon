package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.Stat
import io.github.lishangbu.avalon.dataset.entity.dto.StatSpecification
import jakarta.annotation.Resource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.springframework.transaction.annotation.Transactional

@Transactional
class StatRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var statRepository: StatRepository

    @Test
    fun shouldListAndCrudStat() {
        val condition = StatSpecification(internalName = "attack")

        val results = statRepository.findAll(condition)

        assertFalse(results.isEmpty())
        assertEquals("2", results.first().id)
        assertEquals("attack", results.first().internalName)
        assertEquals("2", results.first().moveDamageClass?.id)
        assertEquals("physical", results.first().moveDamageClass?.internalName)
        assertEquals("物理", results.first().moveDamageClass?.name)

        val existing = requireNotNull(statRepository.findById(2L))
        assertEquals("attack", existing.internalName)
        assertEquals("物理", existing.moveDamageClass?.name)

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

        val inserted = requireNotNull(statRepository.findAll(StatSpecification(internalName = "unit-stat")).firstOrNull())
        assertEquals(saved.id.toString(), inserted.id)
        assertEquals("单元测试能力", inserted.name)

        statRepository.save(inserted.toEntity { name = "更新后的能力" })
        val updated = requireNotNull(statRepository.findAll(StatSpecification(internalName = "unit-stat")).firstOrNull())
        assertEquals("更新后的能力", updated.name)

        statRepository.deleteById(saved.id)
        assertTrue(statRepository.findAll(StatSpecification(internalName = "unit-stat")).isEmpty())
    }

    private fun assertTrue(value: Boolean) {
        assertEquals(true, value)
    }
}
