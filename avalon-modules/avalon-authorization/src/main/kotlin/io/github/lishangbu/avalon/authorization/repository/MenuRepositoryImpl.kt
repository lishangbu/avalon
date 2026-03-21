package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.*
import org.babyfish.jimmer.spring.repository.orderBy
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.ilike
import org.babyfish.jimmer.sql.kt.fetcher.newFetcher
import org.springframework.data.domain.Example
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
class MenuRepositoryImpl(
    private val sql: KSqlClient,
) : MenuRepository {
    override fun findAll(example: Example<Menu>?): List<Menu> {
        val probe = example?.probe
        return sql
            .createQuery(Menu::class) {
                probe.readId()?.let { where(table.id eq it) }
                probe.readParentId()?.let { where(table.parentId eq it) }
                probe.readDisabled()?.let { where(table.disabled eq it) }
                probe.readExtra().takeFilter()?.let { where(table.extra ilike "%$it%") }
                probe.readIcon().takeFilter()?.let { where(table.icon ilike "%$it%") }
                probe.readKey().takeFilter()?.let { where(table.key ilike "%$it%") }
                probe.readLabel().takeFilter()?.let { where(table.label ilike "%$it%") }
                probe.readShow()?.let { where(table.show eq it) }
                probe.readPath().takeFilter()?.let { where(table.path ilike "%$it%") }
                probe.readName().takeFilter()?.let { where(table.name ilike "%$it%") }
                probe.readRedirect().takeFilter()?.let { where(table.redirect ilike "%$it%") }
                probe.readComponent().takeFilter()?.let { where(table.component ilike "%$it%") }
                probe.readSortingOrder()?.let { where(table.sortingOrder eq it) }
                probe.readPinned()?.let { where(table.pinned eq it) }
                probe.readShowTab()?.let { where(table.showTab eq it) }
                probe.readEnableMultiTab()?.let { where(table.enableMultiTab eq it) }
                select(table.fetch(MENU_FETCHER))
            }.execute()
    }

    override fun findAll(
        example: Example<Menu>?,
        sort: Sort,
    ): List<Menu> {
        val probe = example?.probe
        return sql
            .createQuery(Menu::class) {
                probe.readId()?.let { where(table.id eq it) }
                probe.readParentId()?.let { where(table.parentId eq it) }
                probe.readDisabled()?.let { where(table.disabled eq it) }
                probe.readExtra().takeFilter()?.let { where(table.extra ilike "%$it%") }
                probe.readIcon().takeFilter()?.let { where(table.icon ilike "%$it%") }
                probe.readKey().takeFilter()?.let { where(table.key ilike "%$it%") }
                probe.readLabel().takeFilter()?.let { where(table.label ilike "%$it%") }
                probe.readShow()?.let { where(table.show eq it) }
                probe.readPath().takeFilter()?.let { where(table.path ilike "%$it%") }
                probe.readName().takeFilter()?.let { where(table.name ilike "%$it%") }
                probe.readRedirect().takeFilter()?.let { where(table.redirect ilike "%$it%") }
                probe.readComponent().takeFilter()?.let { where(table.component ilike "%$it%") }
                probe.readSortingOrder()?.let { where(table.sortingOrder eq it) }
                probe.readPinned()?.let { where(table.pinned eq it) }
                probe.readShowTab()?.let { where(table.showTab eq it) }
                probe.readEnableMultiTab()?.let { where(table.enableMultiTab eq it) }
                orderBy(sort)
                select(table.fetch(MENU_FETCHER))
            }.execute()
    }

    override fun findById(id: Long): Optional<Menu> = Optional.ofNullable(sql.findById(Menu::class, id))

    override fun findAllById(ids: Iterable<Long>): List<Menu> = ids.mapNotNull { sql.findById(Menu::class, it) }

    override fun save(menu: Menu): Menu =
        sql
            .save(menu) {
                val mode = menu.readId()?.let { SaveMode.UPSERT } ?: SaveMode.INSERT_ONLY
                setMode(mode)
            }.modifiedEntity

    override fun saveAndFlush(menu: Menu): Menu = save(menu)

    override fun deleteById(id: Long) {
        sql
            .createDelete(Menu::class) {
                where(table.id eq id)
                disableDissociation()
            }.execute()
    }

    override fun flush() = Unit

    override fun findAllByOrderBySortingOrderAscIdAsc(): List<Menu> =
        sql
            .createQuery(Menu::class) {
                orderBy(Sort.by(Sort.Order.asc("sortingOrder"), Sort.Order.asc("id")))
                select(table.fetch(MENU_FETCHER))
            }.execute()

    override fun findAllByRoleCodes(roleCodes: List<String>): List<Menu> {
        if (roleCodes.isEmpty()) {
            return emptyList()
        }
        val menus =
            roleCodes.flatMap { roleCode ->
                sql
                    .createQuery(Role::class) {
                        where(table.code eq roleCode)
                        select(table.fetch(ROLE_WITH_MENUS_FETCHER))
                    }.execute()
                    .flatMap { it.menus }
            }
        return menus
            .distinctBy { it.id }
            .sortedWith(
                compareByDescending<Menu> { it.sortingOrder ?: Int.MIN_VALUE }
                    .thenByDescending { it.id },
            )
    }

    companion object {
        private val MENU_FETCHER =
            newFetcher(Menu::class).`by` {
                allScalarFields()
            }
        private val ROLE_WITH_MENUS_FETCHER =
            newFetcher(Role::class).`by` {
                allScalarFields()
                menus {
                    allScalarFields()
                }
            }
    }

    private fun Menu?.readId(): Long? = this?.let { runCatching { it.id }.getOrNull() }

    private fun Menu?.readParentId(): Long? = this?.let { runCatching { it.parentId }.getOrNull() }

    private fun Menu?.readDisabled(): Boolean? = this?.let { runCatching { it.disabled }.getOrNull() }

    private fun Menu?.readExtra(): String? = this?.let { runCatching { it.extra }.getOrNull() }

    private fun Menu?.readIcon(): String? = this?.let { runCatching { it.icon }.getOrNull() }

    private fun Menu?.readKey(): String? = this?.let { runCatching { it.key }.getOrNull() }

    private fun Menu?.readLabel(): String? = this?.let { runCatching { it.label }.getOrNull() }

    private fun Menu?.readShow(): Boolean? = this?.let { runCatching { it.show }.getOrNull() }

    private fun Menu?.readPath(): String? = this?.let { runCatching { it.path }.getOrNull() }

    private fun Menu?.readName(): String? = this?.let { runCatching { it.name }.getOrNull() }

    private fun Menu?.readRedirect(): String? = this?.let { runCatching { it.redirect }.getOrNull() }

    private fun Menu?.readComponent(): String? = this?.let { runCatching { it.component }.getOrNull() }

    private fun Menu?.readSortingOrder(): Int? = this?.let { runCatching { it.sortingOrder }.getOrNull() }

    private fun Menu?.readPinned(): Boolean? = this?.let { runCatching { it.pinned }.getOrNull() }

    private fun Menu?.readShowTab(): Boolean? = this?.let { runCatching { it.showTab }.getOrNull() }

    private fun Menu?.readEnableMultiTab(): Boolean? = this?.let { runCatching { it.enableMultiTab }.getOrNull() }
}
