package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.Gender
import jakarta.annotation.Resource
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.data.domain.Example

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

        val saved = genderRepository.saveAndFlush(gender)

        Assertions.assertNotNull(saved.id)
        Assertions.assertTrue(saved.id > 0)
    }

    @Test
    fun shouldFindGenderById() {
        val female = requireNotNull(genderRepository.findById(1L))

        Assertions.assertEquals(1L, female.id)
        Assertions.assertEquals("female", female.internalName)
        Assertions.assertEquals("♀", female.name)
    }

    @Test
    fun shouldUpdateGenderById() {
        val gender =
            genderRepository.saveAndFlush(
                Gender {
                    internalName = "gender-update"
                    name = "原始性别"
                },
            )
        val id = gender.id

        genderRepository.saveAndFlush(
            Gender(gender) {
                name = "更新后的性别"
            },
        )

        val updatedGender = requireNotNull(genderRepository.findById(id))
        Assertions.assertEquals("更新后的性别", updatedGender.name)
    }

    @Test
    fun shouldDeleteGenderById() {
        val gender =
            genderRepository.saveAndFlush(
                Gender {
                    internalName = "gender-delete"
                    name = "待删除性别"
                },
            )
        val deleteRecordId = gender.id

        Assertions.assertNotNull(genderRepository.findById(deleteRecordId))
        genderRepository.deleteById(deleteRecordId)
        genderRepository.flush()
        Assertions.assertNull(genderRepository.findById(deleteRecordId))
    }

    @Test
    fun shouldSelectListWithDynamicCondition() {
        val condition =
            Gender {
                internalName = "female"
            }

        val results = genderRepository.findAll(Example.of(condition))

        Assertions.assertNotNull(results)
        Assertions.assertTrue(results.any { it.name == "♀" })
    }
}
