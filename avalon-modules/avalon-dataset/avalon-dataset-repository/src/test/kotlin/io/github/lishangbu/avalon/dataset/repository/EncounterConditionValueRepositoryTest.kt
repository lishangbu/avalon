package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.EncounterCondition
import io.github.lishangbu.avalon.dataset.entity.EncounterConditionValue
import io.github.lishangbu.avalon.dataset.entity.dto.EncounterConditionValueSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateEncounterConditionValueInput
import jakarta.annotation.Resource
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.transaction.annotation.Transactional

@Transactional
class EncounterConditionValueRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var encounterConditionValueRepository: EncounterConditionValueRepository

    @Test
    fun shouldListAndCrudEncounterConditionValue() {
        val condition = EncounterConditionValueSpecification(internalName = "swarm-yes")

        val results = encounterConditionValueRepository.listViews(condition)

        assertFalse(results.isEmpty())
        assertEquals("1", results.first().id)
        assertEquals("swarm-yes", results.first().internalName)
        assertEquals("1", results.first().encounterCondition?.id)
        assertEquals("swarm", results.first().encounterCondition?.internalName)

        val seeded = requireNotNull(encounterConditionValueRepository.loadViewById(3L))
        assertEquals("time-morning", seeded.internalName)
        assertEquals("2", seeded.encounterCondition?.id)
        assertEquals("Time of day", seeded.encounterCondition?.name)

        val saved =
            encounterConditionValueRepository.save(
                EncounterConditionValue {
                    internalName = "unit-encounter-condition-value"
                    name = "单元测试遭遇条件值"
                    encounterCondition =
                        EncounterCondition {
                            id = 1L
                        }
                },
                SaveMode.INSERT_ONLY,
            )

        val inserted = requireNotNull(encounterConditionValueRepository.loadViewById(saved.id))
        assertEquals("单元测试遭遇条件值", inserted.name)
        assertEquals("1", inserted.encounterCondition?.id)
        assertEquals("Swarm", inserted.encounterCondition?.name)

        val insertedEntity = requireNotNull(encounterConditionValueRepository.findNullable(saved.id))
        encounterConditionValueRepository.save(
            EncounterConditionValue(insertedEntity) {
                name = "更新后的遭遇条件值"
                encounterCondition =
                    EncounterCondition {
                        id = 2L
                    }
            },
            SaveMode.UPSERT,
        )
        val updated = requireNotNull(encounterConditionValueRepository.loadViewById(saved.id))
        assertEquals("更新后的遭遇条件值", updated.name)
        assertEquals("2", updated.encounterCondition?.id)
        assertEquals("Time of day", updated.encounterCondition?.name)

        encounterConditionValueRepository.deleteById(saved.id)
        assertNull(encounterConditionValueRepository.loadViewById(saved.id))
    }

    @Test
    fun shouldUpdateEncounterConditionValueFromFlatInputPayload() {
        val command =
            UpdateEncounterConditionValueInput(
                id = "1",
                internalName = "swarm-yes",
                name = "During a swarm",
                encounterConditionId = "2",
            )

        encounterConditionValueRepository.save(command.toEntity(), SaveMode.UPSERT)

        val updated = requireNotNull(encounterConditionValueRepository.loadViewById(1L))
        assertEquals("1", updated.id)
        assertEquals("swarm-yes", updated.internalName)
        assertEquals("2", updated.encounterCondition?.id)
        assertTrue(updated.encounterCondition?.name == "Time of day")
    }
}
