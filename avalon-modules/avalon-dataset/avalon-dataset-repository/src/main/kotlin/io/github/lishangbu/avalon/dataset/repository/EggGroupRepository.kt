package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.EggGroupSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.EggGroupView
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.spring.repository.orderBy
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Sort

/** 蛋组仓储接口 */
interface EggGroupRepository : KRepository<EggGroup, Long> {
    /** 按条件查询蛋组视图列表 */
    fun listViews(specification: EggGroupSpecification?): List<EggGroupView> =
        sql
            .createQuery(EggGroup::class) {
                specification?.let(::where)
                orderBy(DEFAULT_SORT)
                select(table.fetch(EggGroupView::class))
            }.execute()

    /** 按 ID 查询蛋组视图 */
    fun loadViewById(id: Long): EggGroupView? =
        sql
            .createQuery(EggGroup::class) {
                where(table.id eq id)
                orderBy(DEFAULT_SORT)
                select(table.fetch(EggGroupView::class))
            }.execute()
            .firstOrNull()

    companion object {
        private val DEFAULT_SORT: Sort = Sort.by(Sort.Order.asc("id"))
    }
}
