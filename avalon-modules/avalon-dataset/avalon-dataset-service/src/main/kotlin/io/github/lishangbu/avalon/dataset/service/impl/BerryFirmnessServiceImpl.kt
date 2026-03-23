package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.BerryFirmness
import io.github.lishangbu.avalon.dataset.repository.BerryFirmnessRepository
import io.github.lishangbu.avalon.dataset.service.BerryFirmnessService
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Example
import org.springframework.data.domain.ExampleMatcher
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

/** 树果硬度服务实现 */
@Service
class BerryFirmnessServiceImpl(
    /** 树果硬度仓储 */
    private val berryFirmnessRepository: BerryFirmnessRepository,
) : BerryFirmnessService {
    /** 按条件分页查询树果硬度*/
    override fun getPageByCondition(
        berryFirmness: BerryFirmness,
        pageable: Pageable,
    ): Page<BerryFirmness> =
        berryFirmnessRepository.findAll(
            Example.of(
                berryFirmness,
                ExampleMatcher
                    .matching()
                    .withMatcher("name", ExampleMatcher.GenericPropertyMatchers.contains())
                    .withMatcher("internalName", ExampleMatcher.GenericPropertyMatchers.contains())
                    .withIgnoreNullValues(),
            ),
            pageable,
        )

    /** 根据条件查询树果硬度列表 */
    override fun listByCondition(berryFirmness: BerryFirmness): List<BerryFirmness> {
        val matcher =
            ExampleMatcher
                .matching()
                .withIgnoreNullValues()
                .withMatcher("name", ExampleMatcher.GenericPropertyMatchers.contains())
                .withMatcher("internalName", ExampleMatcher.GenericPropertyMatchers.contains())
        return berryFirmnessRepository.findAll(Example.of(berryFirmness, matcher))
    }

    /** 保存树果硬度 */
    override fun save(berryFirmness: BerryFirmness): BerryFirmness = berryFirmnessRepository.save(berryFirmness)

    /** 更新树果硬度 */
    override fun update(berryFirmness: BerryFirmness): BerryFirmness = berryFirmnessRepository.save(berryFirmness)

    /** 按 ID 删除树果硬度 */
    override fun removeById(id: Long) {
        berryFirmnessRepository.deleteById(id)
    }
}
