package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.MoveDamageClassSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.MoveDamageClassView
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Pageable

/**
 * 招式伤害分类仓储接口
 *
 * 定义招式伤害分类数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2025/09/14
 */
interface MoveDamageClassRepository : KRepository<MoveDamageClass, Long> {
    /** 按条件查询招式伤害分类视图列表 */
    fun listViews(specification: MoveDamageClassSpecification?): List<MoveDamageClassView> =
        sql
            .createQuery(MoveDamageClass::class) {
                specification?.let(::where)
                select(table.fetch(MoveDamageClassView::class))
            }.execute()

    /** 按条件分页查询招式伤害分类视图 */
    fun pageViews(
        specification: MoveDamageClassSpecification?,
        pageable: Pageable,
    ): Page<MoveDamageClassView> =
        sql
            .createQuery(MoveDamageClass::class) {
                specification?.let(::where)
                select(table.fetch(MoveDamageClassView::class))
            }.fetchPage(pageable.pageNumber, pageable.pageSize)

    /** 按 ID 查询招式伤害分类视图 */
    fun loadViewById(id: Long): MoveDamageClassView? =
        sql
            .createQuery(MoveDamageClass::class) {
                where(table.id eq id)
                select(table.fetch(MoveDamageClassView::class))
            }.execute()
            .firstOrNull()
}
