package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.*
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.ilike
import org.babyfish.jimmer.sql.kt.fetcher.newFetcher
import org.springframework.data.domain.Example
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
class RoleRepositoryImpl(
    private val sql: KSqlClient,
) : RoleRepository {
    override fun findAll(example: Example<Role>?): List<Role> {
        val probe = example?.probe
        return sql
            .createQuery(Role::class) {
                probe.readId()?.let { where(table.id eq it) }
                probe.readCode().takeFilter()?.let { where(table.code ilike "%$it%") }
                probe.readName().takeFilter()?.let { where(table.name ilike "%$it%") }
                probe.readEnabled()?.let { where(table.enabled eq it) }
                select(table.fetch(ROLE_WITH_MENUS_FETCHER))
            }.execute()
    }

    override fun findAll(
        example: Example<Role>?,
        pageable: Pageable,
    ): Page<Role> {
        val probe = example?.probe
        return sql
            .createQuery(Role::class) {
                probe.readId()?.let { where(table.id eq it) }
                probe.readCode().takeFilter()?.let { where(table.code ilike "%$it%") }
                probe.readName().takeFilter()?.let { where(table.name ilike "%$it%") }
                probe.readEnabled()?.let { where(table.enabled eq it) }
                select(table.fetch(ROLE_WITH_MENUS_FETCHER))
            }.fetchPage(pageable.pageNumber, pageable.pageSize)
    }

    override fun findById(id: Long): Optional<Role> =
        Optional.ofNullable(
            sql
                .createQuery(Role::class) {
                    where(table.id eq id)
                    select(table.fetch(ROLE_WITH_MENUS_FETCHER))
                }.execute()
                .firstOrNull(),
        )

    override fun findAllById(ids: Iterable<Long>): List<Role> = ids.mapNotNull { sql.findById(Role::class, it) }

    override fun save(role: Role): Role =
        sql
            .save(role) {
                val mode = role.readId()?.let { SaveMode.UPSERT } ?: SaveMode.INSERT_ONLY
                setMode(mode)
            }.modifiedEntity

    override fun saveAndFlush(role: Role): Role = save(role)

    override fun deleteById(id: Long) {
        sql
            .createDelete(Role::class) {
                where(table.id eq id)
                disableDissociation()
            }.execute()
    }

    override fun flush() = Unit

    companion object {
        private val ROLE_WITH_MENUS_FETCHER =
            newFetcher(Role::class).`by` {
                allScalarFields()
                menus {
                    allScalarFields()
                }
            }
    }

    private fun Role?.readId(): Long? = readOrNull { id }

    private fun Role?.readCode(): String? = readOrNull { code }

    private fun Role?.readName(): String? = readOrNull { name }

    private fun Role?.readEnabled(): Boolean? = readOrNull { enabled }
}
