package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.BerryFirmness
import io.github.lishangbu.avalon.dataset.entity.dto.BerryFirmnessSpecification
import jakarta.annotation.Resource
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest

/** 树果硬度仓储测试 */
class BerryFirmnessRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var berryFirmnessRepository: BerryFirmnessRepository

    /** 验证可按 ID 查询树果硬度 */
    @Test
    fun shouldFindBerryFirmnessById() {
        // Act
        val berryFirmness = requireNotNull(berryFirmnessRepository.findById(1L))

        // Assert
        Assertions.assertEquals(1L, berryFirmness.id)
        Assertions.assertEquals("very-soft", berryFirmness.internalName)
        Assertions.assertEquals("很柔软", berryFirmness.name)
    }

    /** 验证可按 internalName 查询树果硬度 */
    @Test
    fun shouldSelectListMatchByInternalName() {
        val specification = BerryFirmnessSpecification(internalName = "hard")

        val results = berryFirmnessRepository.findAll(specification)

        Assertions.assertNotNull(results)
        Assertions.assertFalse(results.isEmpty(), "应至少匹配到一条属性为坚硬的树果硬度记录")
        Assertions.assertTrue(results.any { (it.internalName ?: "").contains("hard") })
    }

    /** 验证空条件下返回全部预加载记录 */
    @Test
    fun shouldReturnAllPreloadedWhenNoCondition() {
        val all = berryFirmnessRepository.findAll()
        Assertions.assertNotNull(all)
        Assertions.assertTrue(all.size >= 5, "预期至少有 5 条预加载的树果硬度记录")
    }

    /** 验证可按条件分页查询树果硬度 */
    @Test
    fun shouldQueryPageByCondition() {
        val page =
            berryFirmnessRepository.findAll(
                BerryFirmnessSpecification(internalName = "hard"),
                PageRequest.of(0, 10),
            )

        Assertions.assertTrue(page.totalRowCount >= 1)
        Assertions.assertTrue(page.rows.any { (it.internalName ?: "").contains("hard") })
    }

    /** 验证可插入树果硬度并查询 */
    @Test
    fun shouldInsertBerryFirmness() {
        val bf =
            BerryFirmness {
                internalName = "moderate"
                name = "中等"
            }

        val saved = berryFirmnessRepository.save(bf)
        Assertions.assertNotNull(saved.id)

        val berryFirmness = requireNotNull(berryFirmnessRepository.findById(saved.id))
        Assertions.assertEquals("moderate", berryFirmness.internalName)
        Assertions.assertEquals("中等", berryFirmness.name)
    }

    /** 验证可更新树果硬度 */
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
        val updatedEntity = requireNotNull(berryFirmnessRepository.findById(id))
        Assertions.assertEquals("非常嫩", updatedEntity.name)
    }

    /** 验证可删除树果硬度 */
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
        val specification = BerryFirmnessSpecification(internalName = "ephemeral")
        val results = berryFirmnessRepository.findAll(specification)
        Assertions.assertTrue(results.isEmpty(), "删除后按 internalName 查询应返回空集合")
    }
}
