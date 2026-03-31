package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.Berry
import io.github.lishangbu.avalon.dataset.entity.dto.BerrySpecification
import io.github.lishangbu.avalon.dataset.entity.dto.BerryView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveBerryInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateBerryInput
import io.github.lishangbu.avalon.dataset.repository.BerryRepository
import io.github.lishangbu.avalon.dataset.service.BerryService
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

/**
 * 树果应用服务实现
 *
 * 基于仓储封装树果的分页查询与写入逻辑
 */
@Service
class BerryServiceImpl(
    /** 树果仓储 */
    private val berryRepository: BerryRepository,
) : BerryService {
    /** 按筛选条件分页查询树果*/
    override fun getPageByCondition(
        specification: BerrySpecification,
        pageable: Pageable,
    ): Page<BerryView> = berryRepository.pageViews(specification, pageable)

    /** 创建树果 */
    override fun save(command: SaveBerryInput): BerryView = berryRepository.save(command.toEntity(), SaveMode.INSERT_ONLY).let(::reloadView)

    /** 更新树果 */
    override fun update(command: UpdateBerryInput): BerryView = berryRepository.save(command.toEntity(), SaveMode.UPDATE_ONLY).let(::reloadView)

    /** 删除指定 ID 的树果*/
    override fun removeById(id: Long) {
        berryRepository.deleteById(id)
    }

    private fun reloadView(berry: Berry): BerryView = requireNotNull(berryRepository.loadViewById(berry.id)) { "未找到 ID=${berry.id} 对应的树果" }
}
