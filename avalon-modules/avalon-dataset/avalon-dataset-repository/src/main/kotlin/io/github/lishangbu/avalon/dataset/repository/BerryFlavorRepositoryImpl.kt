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

@Repository
class BerryFlavorRepositoryImpl(
    private val sql: KSqlClient,
) : BerryFlavorRepository {
    override fun findAll(): List<BerryFlavor> =
        sql
            .createQuery(BerryFlavor::class) {
                select(table)
            }.execute()

    override fun findAll(example: Example<BerryFlavor>?): List<BerryFlavor> {
        val probe = example?.probe
        return sql
            .createQuery(BerryFlavor::class) {
                probe.readOrNull { id }?.let { where(table.id eq it) }
                probe.readOrNull { name }.takeFilter()?.let { where(table.name ilike "%$it%") }
                probe.readOrNull { internalName }.takeFilter()?.let { where(table.internalName ilike "%$it%") }
                select(table)
            }.execute()
    }

    override fun findAll(
        example: Example<BerryFlavor>?,
        pageable: Pageable,
    ): Page<BerryFlavor> {
        val probe = example?.probe
        return sql
            .createQuery(BerryFlavor::class) {
                probe.readOrNull { id }?.let { where(table.id eq it) }
                probe.readOrNull { name }.takeFilter()?.let { where(table.name ilike "%$it%") }
                probe.readOrNull { internalName }.takeFilter()?.let { where(table.internalName ilike "%$it%") }
                select(table)
            }.fetchPage(pageable.pageNumber, pageable.pageSize)
    }

    override fun findById(id: Long): BerryFlavor? = sql.findById(BerryFlavor::class, id)

    override fun save(berryFlavor: BerryFlavor): BerryFlavor =
        sql
            .save(berryFlavor) {
                val mode = berryFlavor.readOrNull { id }?.let { SaveMode.UPSERT } ?: SaveMode.INSERT_ONLY
                setMode(mode)
            }.modifiedEntity

    override fun saveAndFlush(berryFlavor: BerryFlavor): BerryFlavor = save(berryFlavor)

    override fun deleteById(id: Long) {
        sql
            .createDelete(BerryFlavor::class) {
                where(table.id eq id)
                disableDissociation()
            }.execute()
    }

    override fun flush() = Unit
}
