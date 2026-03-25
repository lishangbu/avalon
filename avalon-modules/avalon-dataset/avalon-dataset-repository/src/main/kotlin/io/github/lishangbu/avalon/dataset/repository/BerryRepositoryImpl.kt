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
) : BerryRepository {
    /** 按条件查询树果列表 */
    override fun findAll(specification: BerrySpecification?): List<Berry> =
        sql
            .createQuery(Berry::class) {
                specification?.let { where(it) }
                select(table)
            }.execute()

    /** 按条件分页查询树果 */
    override fun findAll(
        specification: BerrySpecification?,
        pageable: Pageable,
    ): Page<Berry> =
        sql
            .createQuery(Berry::class) {
                specification?.let { where(it) }
                select(table)
            }.fetchPage(pageable.pageNumber, pageable.pageSize)

    /** 按 ID 查询单个树果 */
    override fun findById(id: Long): Berry? = sql.findById(Berry::class, id)

    /** 保存树果 */
    override fun save(berry: Berry): Berry = sql.save(berry).modifiedEntity

    /** 保存树果并立即刷新 */
    override fun saveAndFlush(berry: Berry): Berry = save(berry)

    /** 删除指定 ID 的树果 */
    override fun deleteById(id: Long) {
        sql
            .createDelete(Berry::class) {
                where(table.id eq id)
                disableDissociation()
            }.execute()
    }

    /** 保留与 Spring Data 风格一致的刷新方法 */
    override fun flush() = Unit
}
