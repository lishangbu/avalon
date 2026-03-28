package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.BerrySpecification
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

/**
 * 树果仓储实现
 *
 * 基于 Jimmer 查询并持久化树果数据
 */
@Repository
class BerryRepositoryImpl(
    /** Jimmer SQL 客户端 */
    private val sql: KSqlClient,
) : BerryRepositoryExt {
    /** 按条件查询树果列表 */
    override fun findAll(specification: BerrySpecification?): List<Berry> =
        sql
            .createQuery(Berry::class) {
                specification?.let { where(it) }
                select(table.fetch(DatasetFetchers.BERRY_WITH_ASSOCIATIONS))
            }.execute()

    /** 按条件分页查询树果 */
    override fun findAll(
        specification: BerrySpecification?,
        pageable: Pageable,
    ): Page<Berry> =
        sql
            .createQuery(Berry::class) {
                specification?.let { where(it) }
                select(table.fetch(DatasetFetchers.BERRY_WITH_ASSOCIATIONS))
            }.fetchPage(pageable.pageNumber, pageable.pageSize)

    /** 按 ID 查询单个树果 */
    override fun findByIdWithAssociations(id: Long): Berry? =
        sql
            .createQuery(Berry::class) {
                where(table.id eq id)
                select(table.fetch(DatasetFetchers.BERRY_WITH_ASSOCIATIONS))
            }.execute()
            .firstOrNull()

    /** 删除指定 ID 的树果 */
    override fun removeById(id: Long) {
        sql
            .createDelete(Berry::class) {
                where(table.id eq id)
                disableDissociation()
            }.execute()
    }
}
