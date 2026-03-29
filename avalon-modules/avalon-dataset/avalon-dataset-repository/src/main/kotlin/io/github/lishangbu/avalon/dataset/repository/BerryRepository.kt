package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.Specification
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Pageable

/**
 * 树果仓储接口
 *
 * 定义树果的查询、保存与删除操作
 *
 * @author lishangbu
 * @since 2025/09/14
 */
interface BerryRepository : KRepository<Berry, Long> {
    /** 按条件查询树果列表 */
    fun findAll(specification: Specification<Berry>?): List<Berry> =
        sql
            .createQuery(Berry::class) {
                specification?.let(::where)
                select(table.fetch(DatasetFetchers.BERRY_WITH_ASSOCIATIONS))
            }.execute()

    /** 按条件分页查询树果 */
    fun findAll(
        specification: Specification<Berry>?,
        pageable: Pageable,
    ): Page<Berry> =
        sql
            .createQuery(Berry::class) {
                specification?.let(::where)
                select(table.fetch(DatasetFetchers.BERRY_WITH_ASSOCIATIONS))
            }.fetchPage(pageable.pageNumber, pageable.pageSize)

    /** 按 ID 查询单个树果及其关联 */
    fun loadByIdWithAssociations(id: Long): Berry? =
        sql
            .createQuery(Berry::class) {
                where(table.id eq id)
                select(table.fetch(DatasetFetchers.BERRY_WITH_ASSOCIATIONS))
            }.execute()
            .firstOrNull()
}
