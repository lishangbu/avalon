package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.Type
import io.github.lishangbu.avalon.dataset.entity.id
import io.github.lishangbu.avalon.dataset.entity.internalName
import io.github.lishangbu.avalon.dataset.entity.name
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.ilike
import org.springframework.data.domain.Example
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class TypeRepositoryImpl(
    /** Jimmer SQL 客户端 */
    private val sql: KSqlClient,
) : TypeRepository {
    /** 查询全部属性列表 */
    override fun findAll(): List<Type> =
        sql
            .createQuery(Type::class) {
                select(table)
            }.execute()

    /** 按条件查询属性列表 */
    override fun findAll(example: Example<Type>?): List<Type> {
        val probe = example?.probe
        return sql
            .createQuery(Type::class) {
                probe.readOrNull { id }?.let { where(table.id eq it) }
                probe.readOrNull { name }.takeFilter()?.let { where(table.name ilike "%$it%") }
                probe.readOrNull { internalName }.takeFilter()?.let { where(table.internalName ilike "%$it%") }
                select(table)
            }.execute()
    }

    /** 按条件分页查询属性 */
    override fun findAll(
        example: Example<Type>?,
        pageable: Pageable,
    ): Page<Type> {
        val probe = example?.probe
        return sql
            .createQuery(Type::class) {
                probe.readOrNull { id }?.let { where(table.id eq it) }
                probe.readOrNull { name }.takeFilter()?.let { where(table.name ilike "%$it%") }
                probe.readOrNull { internalName }.takeFilter()?.let { where(table.internalName ilike "%$it%") }
                select(table)
            }.fetchPage(pageable.pageNumber, pageable.pageSize)
    }

    /** 按 ID 查询属性 */
    override fun findById(id: Long): Type? = sql.findById(Type::class, id)

    /** 保存属性 */
    override fun save(type: Type): Type =
        sql
            .save(type) {
                val mode = type.readOrNull { id }?.let { SaveMode.UPSERT } ?: SaveMode.INSERT_ONLY
                setMode(mode)
            }.modifiedEntity

    /** 保存属性并立即刷新 */
    override fun saveAndFlush(type: Type): Type = save(type)

    /** 按 ID 删除属性 */
    override fun deleteById(id: Long) {
        sql
            .createDelete(Type::class) {
                where(table.id eq id)
                disableDissociation()
            }.execute()
    }

    /** 刷新持久化上下文 */
    override fun flush() = Unit
}
