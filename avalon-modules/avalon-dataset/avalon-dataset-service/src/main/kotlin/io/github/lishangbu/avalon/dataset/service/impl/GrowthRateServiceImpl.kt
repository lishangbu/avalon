package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.GrowthRate
import io.github.lishangbu.avalon.dataset.entity.dto.GrowthRateSpecification
import io.github.lishangbu.avalon.dataset.repository.GrowthRateRepository
import io.github.lishangbu.avalon.dataset.service.GrowthRateService
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
    override fun listByCondition(specification: GrowthRateSpecification): List<GrowthRate> = growthRateRepository.findAll(specification)
}
