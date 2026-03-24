package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.GrowthRate
import io.github.lishangbu.avalon.dataset.repository.GrowthRateRepository
import io.github.lishangbu.avalon.dataset.service.GrowthRateService
import org.springframework.data.domain.Example
import org.springframework.data.domain.ExampleMatcher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 成长速率服务实现 */
@Service
class GrowthRateServiceImpl(
    /** 成长速率仓储 */
    private val growthRateRepository: GrowthRateRepository,
) : GrowthRateService {
    /** 保存成长速率 */
    @Transactional(rollbackFor = [Exception::class])
    override fun save(growthRate: GrowthRate): GrowthRate = growthRateRepository.save(growthRate)

    /** 更新成长速率 */
    @Transactional(rollbackFor = [Exception::class])
    override fun update(growthRate: GrowthRate): GrowthRate = growthRateRepository.save(growthRate)

    /** 按 ID 删除成长速率 */
    @Transactional(rollbackFor = [Exception::class])
    override fun removeById(id: Long) {
        growthRateRepository.deleteById(id)
    }

    /** 按条件查询成长速率列表 */
    override fun listByCondition(growthRate: GrowthRate): List<GrowthRate> {
        val matcher =
            ExampleMatcher
                .matching()
                .withIgnoreNullValues()
                .withMatcher("name", ExampleMatcher.GenericPropertyMatchers.contains())
                .withMatcher("internalName", ExampleMatcher.GenericPropertyMatchers.contains())
                .withMatcher("description", ExampleMatcher.GenericPropertyMatchers.contains())
        return growthRateRepository.findAll(Example.of(growthRate, matcher))
    }
}
