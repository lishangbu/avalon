package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.EggGroup
import io.github.lishangbu.avalon.dataset.entity.dto.EggGroupSpecification
import jakarta.annotation.Resource
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EggGroupRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var eggGroupRepository: EggGroupRepository

    @Test
    fun shouldInsertEggGroupSuccessfully() {
        val eggGroup =
            EggGroup {
                internalName = "unit-test-egg-group"
                name = "单元测试蛋组"
                text = "测试文本"
                characteristics = "测试特征"
            }

        val saved = eggGroupRepository.save(eggGroup, SaveMode.INSERT_ONLY)

        assertTrue(saved.id > 0)
    }

    @Test
    fun shouldFindEggGroupById() {
        val eggGroup = requireNotNull(eggGroupRepository.loadViewById(1L))

        assertEquals("1", eggGroup.id)
        assertEquals("monster", eggGroup.internalName)
        assertEquals("怪兽", eggGroup.name)
    }

    @Test
    fun shouldUpdateEggGroupById() {
        val eggGroup =
            eggGroupRepository.save(
                EggGroup {
                    internalName = "egg-group-update"
                    name = "原始蛋组"
                    text = "原始文本"
                    characteristics = "原始特征"
                },
                SaveMode.INSERT_ONLY,
            )
        val id = eggGroup.id

        eggGroupRepository.save(
            EggGroup(eggGroup) {
                name = "更新后的蛋组"
            },
            SaveMode.UPSERT,
        )

        val updatedEggGroup = requireNotNull(eggGroupRepository.findNullable(id))
        assertEquals("更新后的蛋组", updatedEggGroup.name)
    }

    @Test
    fun shouldDeleteEggGroupById() {
        val eggGroup =
            eggGroupRepository.save(
                EggGroup {
                    internalName = "egg-group-delete"
                    name = "待删除蛋组"
                    text = "待删除文本"
                    characteristics = "待删除特征"
                },
                SaveMode.INSERT_ONLY,
            )
        val deleteRecordId = eggGroup.id

        assertNotNull(eggGroupRepository.findNullable(deleteRecordId))
        eggGroupRepository.deleteById(deleteRecordId)
        assertNull(eggGroupRepository.findNullable(deleteRecordId))
    }

    @Test
    fun shouldSelectListWithDynamicCondition() {
        val condition = EggGroupSpecification(internalName = "monster")

        val results = eggGroupRepository.listViews(condition)

        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.name == "怪兽" })
    }

    @Test
    fun shouldReturnAllEggGroupsWhenNoCondition() {
        val results = eggGroupRepository.listViews(null)

        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.internalName == "monster" })
    }
}
