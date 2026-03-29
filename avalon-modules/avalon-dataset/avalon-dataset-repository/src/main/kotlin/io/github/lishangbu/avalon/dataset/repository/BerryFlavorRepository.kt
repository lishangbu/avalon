package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.BerryFlavor
import org.babyfish.jimmer.Specification
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 树果风味仓储接口
 *
 * 定义树果风味数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2025/09/14
 */
interface BerryFlavorRepository : KRepository<BerryFlavor, Long> {
    /** 按条件查询树果风味列表 */
    fun findAll(specification: Specification<BerryFlavor>?): List<BerryFlavor> =
        sql
            .createQuery(BerryFlavor::class) {
                specification?.let(::where)
                select(table)
            }.execute()
}
