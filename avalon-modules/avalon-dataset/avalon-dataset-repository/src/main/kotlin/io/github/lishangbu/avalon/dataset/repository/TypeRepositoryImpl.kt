package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.ilike
import org.springframework.data.domain.Example
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class TypeRepositoryImpl(
    private val sql: KSqlClient,
) : TypeRepository {
    override fun findAll(): List<Type> =
        sql
            .createQuery(Type::class) {
                select(table)
            }.execute()

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

    override fun findById(id: Long): Optional<Type> = Optional.ofNullable(sql.findById(Type::class, id))

    override fun save(type: Type): Type =
        sql
            .save(type) {
                val mode = type.readOrNull { id }?.let { SaveMode.UPSERT } ?: SaveMode.INSERT_ONLY
                setMode(mode)
            }.modifiedEntity

    override fun saveAndFlush(type: Type): Type = save(type)

    override fun deleteById(id: Long) {
        sql
            .createDelete(Type::class) {
                where(table.id eq id)
                disableDissociation()
            }.execute()
    }

    override fun flush() = Unit
}
