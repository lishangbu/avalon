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

@Repository
class MenuRepositoryImpl(
    /** Jimmer SQL 客户端 */
    private val sql: KSqlClient,
) : MenuRepository {
    /** 按条件查询菜单列表 */
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

    /** 按条件排序查询菜单列表 */
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

    /** 按 ID 查询菜单 */
    override fun findById(id: Long): Menu? = sql.findById(Menu::class, id)

    /** 按 ID 列表查询菜单 */
    override fun findAllById(ids: Iterable<Long>): List<Menu> = ids.mapNotNull { sql.findById(Menu::class, it) }

    /** 保存菜单 */
    override fun save(menu: Menu): Menu =
        sql
            .save(menu) {
                val mode = menu.readId()?.let { SaveMode.UPSERT } ?: SaveMode.INSERT_ONLY
                setMode(mode)
            }.modifiedEntity

    /** 保存菜单并立即刷新 */
    override fun saveAndFlush(menu: Menu): Menu = save(menu)

    /** 按 ID 删除菜单 */
    override fun deleteById(id: Long) {
        sql
            .createDelete(Menu::class) {
                where(table.id eq id)
                disableDissociation()
            }.execute()
    }

    /** 刷新持久化上下文 */
    override fun flush() = Unit

    /** 按排序顺序和 ID 升序查询菜单列表 */
    override fun findAllByOrderBySortingOrderAscIdAsc(): List<Menu> =
        sql
            .createQuery(Menu::class) {
                orderBy(Sort.by(Sort.Order.asc("sortingOrder"), Sort.Order.asc("id")))
                select(table.fetch(MENU_FETCHER))
            }.execute()

    /** 按角色编码列表查询菜单 */
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
        /** 菜单抓取器 */
        private val MENU_FETCHER =
            newFetcher(Menu::class).`by` {
                allScalarFields()
            }

        /** 角色及菜单列表抓取器 */
        private val ROLE_WITH_MENUS_FETCHER =
            newFetcher(Role::class).`by` {
                allScalarFields()
                menus {
                    allScalarFields()
                }
            }
    }

    /** 安全读取主键 */
    private fun Menu?.readId(): Long? = readOrNull { id }

    /** 读取父节点 ID */
    private fun Menu?.readParentId(): Long? = readOrNull { parentId }

    /** 读取禁用状态 */
    private fun Menu?.readDisabled(): Boolean? = readOrNull { disabled }

    /** 读取扩展信息 */
    private fun Menu?.readExtra(): String? = readOrNull { extra }

    /** 读取图标 */
    private fun Menu?.readIcon(): String? = readOrNull { icon }

    /** 读取密钥 */
    private fun Menu?.readKey(): String? = readOrNull { key }

    /** 读取标签 */
    private fun Menu?.readLabel(): String? = readOrNull { label }

    /** 读取显示 */
    private fun Menu?.readShow(): Boolean? = readOrNull { show }

    /** 读取路径 */
    private fun Menu?.readPath(): String? = readOrNull { path }

    /** 读取名称 */
    private fun Menu?.readName(): String? = readOrNull { name }

    /** 读取重定向 */
    private fun Menu?.readRedirect(): String? = readOrNull { redirect }

    /** 读取组件 */
    private fun Menu?.readComponent(): String? = readOrNull { component }

    /** 读取排序顺序 */
    private fun Menu?.readSortingOrder(): Int? = readOrNull { sortingOrder }

    /** 读取固定 */
    private fun Menu?.readPinned(): Boolean? = readOrNull { pinned }

    /** 读取显示标签页 */
    private fun Menu?.readShowTab(): Boolean? = readOrNull { showTab }

    /** 读取启用多标签页 */
    private fun Menu?.readEnableMultiTab(): Boolean? = readOrNull { enableMultiTab }
}
