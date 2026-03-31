package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureHabitatSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureHabitatView
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.spring.repository.orderBy
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository

/** 生物栖息地仓储接口 */
@Repository
interface CreatureHabitatRepository : KRepository<CreatureHabitat, Long> {
    /** 按条件查询生物栖息地视图 */
    fun listViews(specification: CreatureHabitatSpecification?): List<CreatureHabitatView> =
        sql
            .createQuery(CreatureHabitat::class) {
                specification?.let(::where)
                orderBy(DEFAULT_SORT)
                select(table.fetch(CreatureHabitatView::class))
            }.execute()

    /** 按 ID 查询生物栖息地视图 */
    fun loadViewById(id: Long): CreatureHabitatView? =
        sql
            .createQuery(CreatureHabitat::class) {
                where(table.id eq id)
                orderBy(DEFAULT_SORT)
                select(table.fetch(CreatureHabitatView::class))
            }.execute()
            .firstOrNull()

    companion object {
        private val DEFAULT_SORT: Sort = Sort.by(Sort.Order.asc("id"))
    }
}
