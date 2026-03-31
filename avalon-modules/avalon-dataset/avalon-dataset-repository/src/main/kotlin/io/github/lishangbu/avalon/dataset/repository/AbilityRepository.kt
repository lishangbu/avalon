package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.AbilitySpecification
import io.github.lishangbu.avalon.dataset.entity.dto.AbilityView
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.spring.repository.orderBy
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Sort

/** 特性仓储接口 */
interface AbilityRepository : KRepository<Ability, Long> {
    fun listAll(): List<Ability> =
        sql
            .createQuery(Ability::class) {
                orderBy(DEFAULT_SORT)
                select(table)
            }.execute()

    /** 按条件查询特性视图 */
    fun listViews(specification: AbilitySpecification?): List<AbilityView> =
        sql
            .createQuery(Ability::class) {
                specification?.let(::where)
                orderBy(DEFAULT_SORT)
                select(table.fetch(AbilityView::class))
            }.execute()

    /** 按 ID 查询特性视图 */
    fun loadViewById(id: Long): AbilityView? =
        sql
            .createQuery(Ability::class) {
                where(table.id eq id)
                orderBy(DEFAULT_SORT)
                select(table.fetch(AbilityView::class))
            }.execute()
            .firstOrNull()

    companion object {
        private val DEFAULT_SORT: Sort = Sort.by(Sort.Order.asc("id"))
    }
}
