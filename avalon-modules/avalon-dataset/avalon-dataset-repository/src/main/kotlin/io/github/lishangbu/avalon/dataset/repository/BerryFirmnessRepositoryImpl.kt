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
class BerryFirmnessRepositoryImpl(
    private val sql: KSqlClient,
) : BerryFirmnessRepository {
    override fun findAll(): List<BerryFirmness> =
        sql
            .createQuery(BerryFirmness::class) {
                select(table)
            }.execute()

    override fun findAll(example: Example<BerryFirmness>?): List<BerryFirmness> {
        val probe = example?.probe
        return sql
            .createQuery(BerryFirmness::class) {
                probe.readOrNull { id }?.let { where(table.id eq it) }
                probe.readOrNull { name }.takeFilter()?.let { where(table.name ilike "%$it%") }
                probe.readOrNull { internalName }.takeFilter()?.let { where(table.internalName ilike "%$it%") }
                select(table)
            }.execute()
    }

    override fun findAll(
        example: Example<BerryFirmness>?,
        pageable: Pageable,
    ): Page<BerryFirmness> {
        val probe = example?.probe
        return sql
            .createQuery(BerryFirmness::class) {
                probe.readOrNull { id }?.let { where(table.id eq it) }
                probe.readOrNull { name }.takeFilter()?.let { where(table.name ilike "%$it%") }
                probe.readOrNull { internalName }.takeFilter()?.let { where(table.internalName ilike "%$it%") }
                select(table)
            }.fetchPage(pageable.pageNumber, pageable.pageSize)
    }

    override fun findById(id: Long): Optional<BerryFirmness> = Optional.ofNullable(sql.findById(BerryFirmness::class, id))

    override fun save(berryFirmness: BerryFirmness): BerryFirmness =
        sql
            .save(berryFirmness) {
                val mode = berryFirmness.readOrNull { id }?.let { SaveMode.UPSERT } ?: SaveMode.INSERT_ONLY
                setMode(mode)
            }.modifiedEntity

    override fun saveAndFlush(berryFirmness: BerryFirmness): BerryFirmness = save(berryFirmness)

    override fun deleteById(id: Long) {
        sql
            .createDelete(BerryFirmness::class) {
                where(table.id eq id)
                disableDissociation()
            }.execute()
    }

    override fun flush() = Unit

    private fun String?.takeFilter(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    private inline fun <T, R> T?.readOrNull(block: T.() -> R): R? = this?.let { runCatching { it.block() }.getOrNull() }
}
