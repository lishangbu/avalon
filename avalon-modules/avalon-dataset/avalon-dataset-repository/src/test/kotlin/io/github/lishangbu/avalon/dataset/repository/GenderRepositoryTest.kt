package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.Gender
import io.github.lishangbu.avalon.dataset.entity.dto.GenderSpecification
import jakarta.annotation.Resource
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder

/** 性别仓储测试 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class GenderRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var genderRepository: GenderRepository

    @Test
    fun shouldInsertGenderSuccessfully() {
        val gender =
            Gender {
                internalName = "unit-test-gender"
                name = "单元测试性别"
            }

        val saved = genderRepository.save(gender, SaveMode.INSERT_ONLY)

        Assertions.assertNotNull(saved.id)
        Assertions.assertTrue(saved.id > 0)
    }

    @Test
    fun shouldFindGenderById() {
        val female = requireNotNull(genderRepository.findNullable(1L))

        Assertions.assertEquals(1L, female.id)
        Assertions.assertEquals("female", female.internalName)
        Assertions.assertEquals("♀", female.name)
    }

    @Test
    fun shouldUpdateGenderById() {
        val gender =
            genderRepository.save(
                Gender {
                    internalName = "gender-update"
                    name = "原始性别"
                },
                SaveMode.INSERT_ONLY,
            )
        val id = gender.id

        genderRepository.save(
            Gender(gender) {
                name = "更新后的性别"
            },
            SaveMode.UPSERT,
        )

        val updatedGender = requireNotNull(genderRepository.findNullable(id))
        Assertions.assertEquals("更新后的性别", updatedGender.name)
    }

    @Test
    fun shouldDeleteGenderById() {
        val gender =
            genderRepository.save(
                Gender {
                    internalName = "gender-delete"
                    name = "待删除性别"
                },
                SaveMode.INSERT_ONLY,
            )
        val deleteRecordId = gender.id

        Assertions.assertNotNull(genderRepository.findNullable(deleteRecordId))
        genderRepository.deleteById(deleteRecordId)
        Assertions.assertNull(genderRepository.findNullable(deleteRecordId))
    }

    @Test
    fun shouldSelectListWithDynamicCondition() {
        val condition = GenderSpecification(internalName = "female")

        val results = genderRepository.findAll(condition)

        Assertions.assertNotNull(results)
        Assertions.assertTrue(results.any { it.name == "♀" })
    }

    @Test
    fun shouldReturnAllGendersWhenNoCondition() {
        val results = genderRepository.findAll()

        Assertions.assertNotNull(results)
        Assertions.assertTrue(results.size >= 3)
        Assertions.assertTrue(results.any { it.internalName == "female" })
    }
}
