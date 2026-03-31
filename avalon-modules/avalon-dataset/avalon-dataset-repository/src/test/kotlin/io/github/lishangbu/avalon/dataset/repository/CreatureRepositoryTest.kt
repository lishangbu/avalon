package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.Creature
import io.github.lishangbu.avalon.dataset.entity.CreatureSpecies
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureSpecification
import jakarta.annotation.Resource
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Transactional

@Transactional
class CreatureRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var creatureRepository: CreatureRepository

    @Test
    fun shouldQueryPageAndCrudCreature() {
        val condition = CreatureSpecification(internalName = "bulbasaur", creatureSpeciesId = "1")

        val results = creatureRepository.listViews(condition)
        val page = creatureRepository.pageViews(condition, PageRequest.of(0, 10))

        assertFalse(results.isEmpty())
        assertEquals("1", results.first().id)
        assertEquals("bulbasaur", results.first().internalName)
        assertEquals("妙蛙种子", results.first().creatureSpecies?.name)
        assertTrue(page.totalRowCount >= 1)
        assertFalse(page.rows.isEmpty())

        val created =
            creatureRepository.save(
                Creature {
                    internalName = "testmon"
                    name = "测试宝可梦"
                    height = 10
                    weight = 100
                    baseExperience = 200
                    sortingOrder = 9999
                    creatureSpecies =
                        CreatureSpecies {
                            id = 1L
                        }
                },
                SaveMode.INSERT_ONLY,
            )

        val createdView = requireNotNull(creatureRepository.loadViewById(created.id))
        assertEquals("testmon", createdView.internalName)
        assertEquals("1", createdView.creatureSpecies?.id)

        val existing = requireNotNull(creatureRepository.findNullable(created.id))
        creatureRepository.save(Creature(existing) { name = "更新后的测试宝可梦" }, SaveMode.UPSERT)

        val updated = requireNotNull(creatureRepository.loadViewById(created.id))
        assertEquals("更新后的测试宝可梦", updated.name)

        creatureRepository.deleteById(created.id)
        assertNull(creatureRepository.loadViewById(created.id))
    }
}
