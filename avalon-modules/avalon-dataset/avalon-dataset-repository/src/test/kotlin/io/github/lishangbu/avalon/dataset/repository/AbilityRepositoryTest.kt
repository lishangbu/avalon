package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.Ability
import io.github.lishangbu.avalon.dataset.entity.dto.AbilitySpecification
import jakarta.annotation.Resource
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AbilityRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var abilityRepository: AbilityRepository

    @Test
    fun shouldInsertAbilitySuccessfully() {
        val ability =
            Ability {
                internalName = "unit-test-ability"
                name = "单元测试特性"
                effect = "测试效果"
                introduction = "测试介绍"
            }

        val saved = abilityRepository.save(ability, SaveMode.INSERT_ONLY)

        assertTrue(saved.id > 0)
    }

    @Test
    fun shouldFindAbilityById() {
        val ability = requireNotNull(abilityRepository.findNullable(1L))

        assertEquals(1L, ability.id)
        assertEquals("stench", ability.internalName)
        assertEquals("恶臭", ability.name)
    }

    @Test
    fun shouldUpdateAbilityById() {
        val ability =
            abilityRepository.save(
                Ability {
                    internalName = "ability-update"
                    name = "原始特性"
                    effect = "原始效果"
                    introduction = "原始介绍"
                },
                SaveMode.INSERT_ONLY,
            )
        val id = ability.id

        abilityRepository.save(
            Ability(ability) {
                name = "更新后的特性"
            },
            SaveMode.UPSERT,
        )

        val updatedAbility = requireNotNull(abilityRepository.findNullable(id))
        assertEquals("更新后的特性", updatedAbility.name)
    }

    @Test
    fun shouldDeleteAbilityById() {
        val ability =
            abilityRepository.save(
                Ability {
                    internalName = "ability-delete"
                    name = "待删除特性"
                    effect = "待删除效果"
                    introduction = "待删除介绍"
                },
                SaveMode.INSERT_ONLY,
            )
        val deleteRecordId = ability.id

        assertNotNull(abilityRepository.findNullable(deleteRecordId))
        abilityRepository.deleteById(deleteRecordId)
        assertNull(abilityRepository.findNullable(deleteRecordId))
    }

    @Test
    fun shouldSelectListWithDynamicCondition() {
        val condition = AbilitySpecification(internalName = "stench")

        val results = abilityRepository.findAll(condition)

        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.name == "恶臭" })
    }

    @Test
    fun shouldReturnAllAbilitiesWhenNoCondition() {
        val results = abilityRepository.findAll()

        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.internalName == "stench" })
    }
}
