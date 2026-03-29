package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.BerryFirmness
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.Specification
import org.babyfish.jimmer.spring.repository.KRepository
import org.springframework.data.domain.Pageable

/**
 * 树果硬度仓储接口
 *
 * 定义树果硬度数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2025/09/14
 */
interface BerryFirmnessRepository : KRepository<BerryFirmness, Long> {
    /** 按条件查询树果硬度列表 */
    fun findAll(specification: Specification<BerryFirmness>?): List<BerryFirmness> =
        sql
            .createQuery(BerryFirmness::class) {
                specification?.let(::where)
                select(table)
            }.execute()

    /** 按条件分页查询树果硬度 */
    fun findAll(
        specification: Specification<BerryFirmness>?,
        pageable: Pageable,
    ): Page<BerryFirmness> =
        sql
            .createQuery(BerryFirmness::class) {
                specification?.let(::where)
                select(table)
            }.fetchPage(pageable.pageNumber, pageable.pageSize)
}
