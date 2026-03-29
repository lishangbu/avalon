package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.dto.BerryFlavorSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.BerryFlavorView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveBerryFlavorInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateBerryFlavorInput
import io.github.lishangbu.avalon.dataset.repository.BerryFlavorRepository
import io.github.lishangbu.avalon.dataset.service.BerryFlavorService
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.stereotype.Service

/** 树果风味服务实现 */
@Service
class BerryFlavorServiceImpl(
    /** 树果风味仓储 */
    private val berryFlavorRepository: BerryFlavorRepository,
) : BerryFlavorService {
    /** 保存树果风味 */
    override fun save(command: SaveBerryFlavorInput): BerryFlavorView = BerryFlavorView(berryFlavorRepository.save(command.toEntity(), SaveMode.INSERT_ONLY))

    /** 更新树果风味 */
    override fun update(command: UpdateBerryFlavorInput): BerryFlavorView = BerryFlavorView(berryFlavorRepository.save(command.toEntity(), SaveMode.UPSERT))

    /** 按 ID 删除树果风味 */
    override fun removeById(id: Long) {
        berryFlavorRepository.deleteById(id)
    }

    /** 按条件查询树果风味列表 */
    override fun listByCondition(
        specification: BerryFlavorSpecification,
    ): List<BerryFlavorView> = berryFlavorRepository.findAll(specification).map(::BerryFlavorView)
}
