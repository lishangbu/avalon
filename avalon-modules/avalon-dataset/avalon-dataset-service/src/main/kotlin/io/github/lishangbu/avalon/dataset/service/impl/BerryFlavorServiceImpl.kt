package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.BerryFlavor
import io.github.lishangbu.avalon.dataset.entity.dto.BerryFlavorSpecification
import io.github.lishangbu.avalon.dataset.repository.BerryFlavorRepository
import io.github.lishangbu.avalon.dataset.service.BerryFlavorService
import org.springframework.stereotype.Service

/** 树果风味服务实现 */
@Service
class BerryFlavorServiceImpl(
    /** 树果风味仓储 */
    private val berryFlavorRepository: BerryFlavorRepository,
) : BerryFlavorService {
    /** 保存树果风味 */
    override fun save(berryFlavor: BerryFlavor): BerryFlavor = berryFlavorRepository.save(berryFlavor)

    /** 更新树果风味 */
    override fun update(berryFlavor: BerryFlavor): BerryFlavor = berryFlavorRepository.save(berryFlavor)

    /** 按 ID 删除树果风味 */
    override fun removeById(id: Long) {
        berryFlavorRepository.deleteById(id)
    }

    /** 按条件查询树果风味列表 */
    override fun listByCondition(specification: BerryFlavorSpecification): List<BerryFlavor> = berryFlavorRepository.findAll(specification)
}
