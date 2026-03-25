package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.BerryFlavor
import io.github.lishangbu.avalon.dataset.entity.dto.BerryFlavorSpecification
import jakarta.annotation.Resource
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/** 树果风味仓储测试 */
class BerryFlavorRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var berryFlavorRepository: BerryFlavorRepository

    /** 验证可按 ID 查询树果风味 */
    @Test
    fun shouldFindBerryFlavorById() {
        // Act
        val flavor = requireNotNull(berryFlavorRepository.findById(1L))
        // Assert
        Assertions.assertEquals(1L, flavor.id)
        Assertions.assertEquals("spicy", flavor.internalName)
        Assertions.assertEquals("辣", flavor.name)
    }

    /** 验证可按 internalName 查询树果风味 */
    @Test
    fun shouldSelectListMatchByInternalName() {
        // Arrange - 构造查询条件，使用部分 internalName
        val specification = BerryFlavorSpecification(internalName = "spicy")

        // Act
        val results = berryFlavorRepository.findAll(specification)

        // Assert
        Assertions.assertNotNull(results)
        Assertions.assertFalse(results.isEmpty(), "应至少匹配到一条属性为辣的树果风味记录")
        Assertions.assertTrue(results.any { it.internalName == "spicy" })
    }

    /** 验证空条件下返回全部预加载记录 */
    @Test
    fun shouldReturnAllPreloadedBerryFlavorsWhenNoCondition() {
        // Act
        val all = berryFlavorRepository.findAll()

        // Assert
        Assertions.assertNotNull(all)
        // 根据 db/changelog 数据文件，至少有 5 条记录
        Assertions.assertTrue(all.size >= 5, "预期至少有 5 个预加载的树果风味记录")
    }

    /** 验证可插入树果风味 */
    @Test
    fun shouldInsertBerryFlavor() {
        // Arrange
        val bf =
            BerryFlavor {
                internalName = "savory"
                name = "鲜美"
            }

        // Act
        val saved = berryFlavorRepository.save(bf)

        // Assert
        Assertions.assertNotNull(saved.id)
        val berryFlavor = requireNotNull(berryFlavorRepository.findById(saved.id))
        Assertions.assertEquals("savory", berryFlavor.internalName)
        Assertions.assertEquals("鲜美", berryFlavor.name)
    }

    /** 验证可按 ID 更新树果风味名称 */
    @Test
    fun shouldUpdateBerryFlavorById() {
        // Arrange - 先插入一条临时记录
        val created =
            berryFlavorRepository.saveAndFlush(
                BerryFlavor {
                    internalName = "fishy"
                    name = "鱼腥"
                },
            )
        val id = created.id

        // Act
        berryFlavorRepository.saveAndFlush(BerryFlavor(created) { name = "鱼腥味" })

        // Assert
        val updatedEntity = requireNotNull(berryFlavorRepository.findById(id))
        Assertions.assertEquals("鱼腥味", updatedEntity.name)
    }

    /** 验证可按 ID 删除树果风味 */
    @Test
    fun shouldDeleteBerryFlavorById() {
        // Arrange - 插入临时记录
        val created =
            berryFlavorRepository.saveAndFlush(
                BerryFlavor {
                    internalName = "pungent"
                    name = "刺激性风味"
                },
            )
        val id = created.id

        // Act
        berryFlavorRepository.deleteById(id)
        berryFlavorRepository.flush()
        // 验证通过 internalName 查询不到该记录
        val specification = BerryFlavorSpecification(internalName = "pungent")
        val results = berryFlavorRepository.findAll(specification)
        Assertions.assertTrue(results.isEmpty(), "删除后按 internalName 查询应返回空集合")
    }
}
