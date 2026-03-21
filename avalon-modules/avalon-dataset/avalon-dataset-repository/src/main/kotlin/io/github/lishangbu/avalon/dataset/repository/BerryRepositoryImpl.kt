package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.ilike
import org.springframework.data.domain.Example
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class BerryRepositoryImpl(
    private val sql: KSqlClient,
) : BerryRepository {
    override fun findAll(example: Example<Berry>?): List<Berry> {
        val probe = example?.probe
        return sql
            .createQuery(Berry::class) {
                probe.readOrNull { id }?.let { where(table.id eq it) }
                probe.readOrNull { name }.takeFilter()?.let { where(table.name ilike "%$it%") }
                probe.readOrNull { internalName }.takeFilter()?.let { where(table.internalName ilike "%$it%") }
                probe.readOrNull { growthTime }?.let { where(table.growthTime eq it) }
                probe.readOrNull { maxHarvest }?.let { where(table.maxHarvest eq it) }
                probe.readOrNull { bulk }?.let { where(table.bulk eq it) }
                probe.readOrNull { smoothness }?.let { where(table.smoothness eq it) }
                probe.readOrNull { soilDryness }?.let { where(table.soilDryness eq it) }
                probe.readOrNull { naturalGiftPower }?.let { where(table.naturalGiftPower eq it) }
                probe.readOrNull { berryFirmnessId }?.let { where(table.berryFirmnessId eq it) }
                probe.readOrNull { naturalGiftTypeId }?.let { where(table.naturalGiftTypeId eq it) }
                select(table)
            }.execute()
    }

    override fun findAll(
        example: Example<Berry>?,
        pageable: Pageable,
    ): Page<Berry> {
        val probe = example?.probe
        return sql
            .createQuery(Berry::class) {
                probe.readOrNull { id }?.let { where(table.id eq it) }
                probe.readOrNull { name }.takeFilter()?.let { where(table.name ilike "%$it%") }
                probe.readOrNull { internalName }.takeFilter()?.let { where(table.internalName ilike "%$it%") }
                probe.readOrNull { growthTime }?.let { where(table.growthTime eq it) }
                probe.readOrNull { maxHarvest }?.let { where(table.maxHarvest eq it) }
                probe.readOrNull { bulk }?.let { where(table.bulk eq it) }
                probe.readOrNull { smoothness }?.let { where(table.smoothness eq it) }
                probe.readOrNull { soilDryness }?.let { where(table.soilDryness eq it) }
                probe.readOrNull { naturalGiftPower }?.let { where(table.naturalGiftPower eq it) }
                probe.readOrNull { berryFirmnessId }?.let { where(table.berryFirmnessId eq it) }
                probe.readOrNull { naturalGiftTypeId }?.let { where(table.naturalGiftTypeId eq it) }
                select(table)
            }.fetchPage(pageable.pageNumber, pageable.pageSize)
    }

    override fun findById(id: Long): Optional<Berry> = Optional.ofNullable(sql.findById(Berry::class, id))

    override fun save(berry: Berry): Berry = sql.save(berry).modifiedEntity

    override fun saveAndFlush(berry: Berry): Berry = save(berry)

    override fun deleteById(id: Long) {
        sql
            .createDelete(Berry::class) {
                where(table.id eq id)
                disableDissociation()
            }.execute()
    }

    override fun flush() = Unit

    private fun String?.takeFilter(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    private inline fun <T, R> T?.readOrNull(block: T.() -> R): R? = this?.let { runCatching { it.block() }.getOrNull() }
}
