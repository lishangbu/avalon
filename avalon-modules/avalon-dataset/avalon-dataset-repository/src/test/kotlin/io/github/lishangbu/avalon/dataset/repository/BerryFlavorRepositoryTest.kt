package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.BerryFlavor
import jakarta.annotation.Resource
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Example

/** 树果风味数据访问层测试 */
class BerryFlavorRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var berryFlavorRepository: BerryFlavorRepository

    /** 验证 通过ID查询 */
    @Test
    fun shouldFindBerryFlavorById() {
        // Act
        val flavorOptional = berryFlavorRepository.findById(1L)
        Assertions.assertTrue(flavorOptional.isPresent)
        val flavor = flavorOptional.get()
        // Assert
        Assertions.assertNotNull(flavor, "应已插入 id=1 的 属性为辣的树果风味记录")
        Assertions.assertEquals(1L, flavor.id)
        Assertions.assertEquals("spicy", flavor.internalName)
        Assertions.assertEquals("辣", flavor.name)
    }

    /** 验证 selectList 的动态查询能够基于部分 internalName 匹配到 数据库中的数据 */
    @Test
    fun shouldSelectListMatchByInternalName() {
        // Arrange - 构造查询条件，使用部分 internalName
        val cond =
            BerryFlavor {
                internalName = "spicy"
            }

        // Act
        val results = berryFlavorRepository.findAll(Example.of(cond))

        // Assert
        Assertions.assertNotNull(results)
        Assertions.assertFalse(results.isEmpty(), "应至少匹配到一条属性为辣的树果风味记录")
        Assertions.assertTrue(results.any { it.internalName == "spicy" })
    }

    /** 验证 selectList 在空条件下返回所有预加载的记录 */
    @Test
    fun shouldReturnAllPreloadedBerryFlavorsWhenNoCondition() {
        // Act
        val all = berryFlavorRepository.findAll()

        // Assert
        Assertions.assertNotNull(all)
        // 根据 db/changelog 数据文件，至少有 5 条记录
        Assertions.assertTrue(all.size >= 5, "预期至少有 5 个预加载的树果风味记录")
    }

    /** 测试插入树果风味（使用简短合理的 internalName 避免与预加载数据冲突） */
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
        val berryFlavorOptional = berryFlavorRepository.findById(saved.id)
        Assertions.assertTrue(berryFlavorOptional.isPresent)
        val berryFlavor = berryFlavorOptional.get()
        Assertions.assertNotNull(berryFlavor)
        Assertions.assertEquals("savory", berryFlavor.internalName)
        Assertions.assertEquals("鲜美", berryFlavor.name)
    }

    /** 测试根据 ID 更新树果风味的名称 */
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
        val updatedEntityOptional = berryFlavorRepository.findById(id)
        Assertions.assertTrue(updatedEntityOptional.isPresent)
        val updatedEntity = updatedEntityOptional.get()
        Assertions.assertNotNull(updatedEntity)
        Assertions.assertEquals("鱼腥味", updatedEntity.name)
    }

    /** 测试根据 ID 删除树果风味 */
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
        val cond =
            BerryFlavor {
                internalName = "pungent"
            }
        val results = berryFlavorRepository.findAll(Example.of(cond))
        Assertions.assertTrue(results.isEmpty(), "删除后按 internalName 查询应返回空集合")
    }
}
