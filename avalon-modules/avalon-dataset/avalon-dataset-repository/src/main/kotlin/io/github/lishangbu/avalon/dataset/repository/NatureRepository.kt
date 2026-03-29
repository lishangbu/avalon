package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import org.babyfish.jimmer.Specification
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.sql.kt.ast.expression.eq

/** 性格仓储接口 */
interface NatureRepository : KRepository<Nature, Long> {
    /** 按条件查询性格列表 */
    fun findAll(specification: Specification<Nature>?): List<Nature> =
        sql
            .createQuery(Nature::class) {
                specification?.let(::where)
                select(table.fetch(DatasetFetchers.NATURE_WITH_ASSOCIATIONS))
            }.execute()

    /** 按 ID 查询性格及其关联 */
    fun loadByIdWithAssociations(id: Long): Nature? =
        sql
            .createQuery(Nature::class) {
                where(table.id eq id)
                select(table.fetch(DatasetFetchers.NATURE_WITH_ASSOCIATIONS))
            }.execute()
            .firstOrNull()
}
