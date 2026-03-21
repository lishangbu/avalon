package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.BerryFirmness
import jakarta.annotation.Resource
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Example

/** 树果硬度数据访问层测试 */
class BerryFirmnessRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var berryFirmnessRepository: BerryFirmnessRepository

    /** 验证 数据库中的数据可以通过 selectById 查询到 */
    @Test
    fun shouldFindBerryFirmnessById() {
        // Act
        val firmnessOptional = berryFirmnessRepository.findById(1L)

        // Assert
        Assertions.assertTrue(firmnessOptional.isPresent, "应已插入 id=1 的 属性为很柔软的树果硬度记录")
        val berryFirmness = firmnessOptional.get()
        Assertions.assertEquals(1L, berryFirmness.id)
        Assertions.assertEquals("very-soft", berryFirmness.internalName)
        Assertions.assertEquals("很柔软", berryFirmness.name)
    }

    /** 验证 selectList 的动态查询能够基于 internalName 匹配到预加载的数据 */
    @Test
    fun shouldSelectListMatchByInternalName() {
        val cond =
            BerryFirmness {
                internalName = "hard"
            }

        val results = berryFirmnessRepository.findAll(Example.of(cond))

        Assertions.assertNotNull(results)
        Assertions.assertFalse(results.isEmpty(), "应至少匹配到一条属性为坚硬的树果硬度记录")
        Assertions.assertTrue(results.any { (it.internalName ?: "").contains("hard") })
    }

    /** 验证在空条件下能返回预加载的所有记录 */
    @Test
    fun shouldReturnAllPreloadedWhenNoCondition() {
        val all = berryFirmnessRepository.findAll()
        Assertions.assertNotNull(all)
        Assertions.assertTrue(all.size >= 5, "预期至少有 5 条预加载的树果硬度记录")
    }

    /** 测试插入树果硬度并查询 */
    @Test
    fun shouldInsertBerryFirmness() {
        val bf =
            BerryFirmness {
                internalName = "moderate"
                name = "中等"
            }

        val saved = berryFirmnessRepository.save(bf)
        Assertions.assertNotNull(saved.id)

        val firmnessOptional = berryFirmnessRepository.findById(saved.id)
        Assertions.assertTrue(firmnessOptional.isPresent)
        val berryFirmness = firmnessOptional.get()
        Assertions.assertEquals("moderate", berryFirmness.internalName)
        Assertions.assertEquals("中等", berryFirmness.name)
    }

    /** 测试更新树果硬度 */
    @Test
    fun shouldUpdateBerryFirmnessById() {
        val created =
            berryFirmnessRepository.saveAndFlush(
                BerryFirmness {
                    internalName = "tender"
                    name = "嫩"
                },
            )
        val id = created.id
        berryFirmnessRepository.saveAndFlush(BerryFirmness(created) { name = "非常嫩" })
        val updatedEntityOptional = berryFirmnessRepository.findById(id)
        Assertions.assertTrue(updatedEntityOptional.isPresent)
        val updatedEntity = updatedEntityOptional.get()
        Assertions.assertEquals("非常嫩", updatedEntity.name)
    }

    /** 测试删除树果硬度 */
    @Test
    fun shouldDeleteBerryFirmnessById() {
        val created =
            berryFirmnessRepository.saveAndFlush(
                BerryFirmness {
                    internalName = "ephemeral"
                    name = "短暂"
                },
            )
        val id = created.id

        berryFirmnessRepository.deleteById(id)
        berryFirmnessRepository.flush()

        // 通过 internalName 查询应该为空
        val cond =
            BerryFirmness {
                internalName = "ephemeral"
            }
        val results = berryFirmnessRepository.findAll(Example.of(cond))
        Assertions.assertTrue(results.isEmpty(), "删除后按 internalName 查询应返回空集合")
    }
}
