package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureSpeciesSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureSpeciesView
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.spring.repository.orderBy
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository

/**
 * 生物种类仓储接口
 *
 * 定义生物种类数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2026/2/12
 */
@Repository
interface CreatureSpeciesRepository : KRepository<CreatureSpecies, Long> {
    /** 按条件查询生物种族视图 */
    fun listViews(specification: CreatureSpeciesSpecification?): List<CreatureSpeciesView> =
        sql
            .createQuery(CreatureSpecies::class) {
                specification?.let(::where)
                orderBy(DEFAULT_SORT)
                select(table.fetch(CreatureSpeciesView::class))
            }.execute()

    /** 按条件分页查询生物种族视图 */
    fun pageViews(
        specification: CreatureSpeciesSpecification?,
        pageable: Pageable,
    ): Page<CreatureSpeciesView> =
        sql
            .createQuery(CreatureSpecies::class) {
                specification?.let(::where)
                orderBy(DEFAULT_SORT)
                select(table.fetch(CreatureSpeciesView::class))
            }.fetchPage(pageable.pageNumber, pageable.pageSize)

    /** 按 ID 查询单个生物种族视图 */
    fun loadViewById(id: Long): CreatureSpeciesView? =
        sql
            .createQuery(CreatureSpecies::class) {
                where(table.id eq id)
                orderBy(DEFAULT_SORT)
                select(table.fetch(CreatureSpeciesView::class))
            }.execute()
            .firstOrNull()

    companion object {
        private val DEFAULT_SORT: Sort = Sort.by(Sort.Order.asc("id"))
    }
}
