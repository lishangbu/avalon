package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.ilike
import org.springframework.data.domain.Example
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class StatRepositoryImpl(
    private val sql: KSqlClient,
) : StatRepository {
    override fun findAll(example: Example<Stat>?): List<Stat> {
        val probe = example?.probe
        return sql
            .createQuery(Stat::class) {
                probe.readOrNull { id }?.let { where(table.id eq it) }
                probe.readOrNull { name }.takeFilter()?.let { where(table.name ilike "%$it%") }
                probe.readOrNull { internalName }.takeFilter()?.let { where(table.internalName ilike "%$it%") }
                probe.readOrNull { gameIndex }?.let { where(table.gameIndex eq it) }
                probe.readOrNull { isBattleOnly }?.let { where(table.isBattleOnly eq it) }
                probe.readOrNull { moveDamageClassId }?.let { where(table.moveDamageClassId eq it) }
                select(table)
            }.execute()
    }

    override fun findAll(
        example: Example<Stat>?,
        pageable: Pageable,
    ): Page<Stat> {
        val probe = example?.probe
        return sql
            .createQuery(Stat::class) {
                probe.readOrNull { id }?.let { where(table.id eq it) }
                probe.readOrNull { name }.takeFilter()?.let { where(table.name ilike "%$it%") }
                probe.readOrNull { internalName }.takeFilter()?.let { where(table.internalName ilike "%$it%") }
                probe.readOrNull { gameIndex }?.let { where(table.gameIndex eq it) }
                probe.readOrNull { isBattleOnly }?.let { where(table.isBattleOnly eq it) }
                probe.readOrNull { moveDamageClassId }?.let { where(table.moveDamageClassId eq it) }
                select(table)
            }.fetchPage(pageable.pageNumber, pageable.pageSize)
    }

    override fun save(stat: Stat): Stat = sql.save(stat).modifiedEntity

    override fun deleteById(id: Long) {
        sql
            .createDelete(Stat::class) {
                where(table.id eq id)
                disableDissociation()
            }.execute()
    }
}
