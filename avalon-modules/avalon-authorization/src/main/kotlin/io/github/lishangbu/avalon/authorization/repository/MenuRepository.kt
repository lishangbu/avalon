package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.*
import io.github.lishangbu.avalon.authorization.entity.dto.MenuTreeView
import io.github.lishangbu.avalon.authorization.entity.dto.MenuView
import org.babyfish.jimmer.Specification
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.spring.repository.orderBy
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.isNull
import org.springframework.data.domain.Sort

/**
 * 菜单仓储接口
 *
 * 定义菜单数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2025/08/20
 */
interface MenuRepository : KRepository<Menu, Long> {
    /** 按条件查询菜单列表 */
    fun findAll(specification: Specification<Menu>?): List<Menu> =
        sql
            .createQuery(Menu::class) {
                specification?.let(::where)
                select(table.fetch(AuthorizationFetchers.MENU))
            }.execute()

    /** 按条件查询菜单视图列表 */
    fun listViews(specification: Specification<Menu>?): List<MenuView> =
        sql
            .createQuery(Menu::class) {
                specification?.let(::where)
                orderBy(DEFAULT_SORT)
                select(table.fetch(MenuView::class))
            }.execute()

    /** 查询根节点菜单树 */
    fun listTreeViews(): List<MenuTreeView> =
        sql
            .createQuery(Menu::class) {
                where(table.parentId.isNull())
                orderBy(DEFAULT_SORT)
                select(table.fetch(MenuTreeView::class))
            }.execute()

    /** 判断是否存在子菜单 */
    fun hasChildren(parentId: Long): Boolean =
        sql
            .createQuery(Menu::class) {
                where(table.parentId eq parentId)
                select(table.id)
            }.execute()
            .isNotEmpty()

    /** 按 ID 查询菜单视图 */
    fun loadViewById(id: Long): MenuView? =
        sql
            .createQuery(Menu::class) {
                where(table.id eq id)
                select(table.fetch(MenuView::class))
            }.execute()
            .firstOrNull()

    /** 按角色编码列表查询授权菜单视图 */
    fun listViewsByRoleCodes(roleCodes: List<String>): List<MenuView> {
        if (roleCodes.isEmpty()) {
            return emptyList()
        }
        val menus =
            roleCodes.flatMap { roleCode ->
                sql
                    .createQuery(Role::class) {
                        where(table.code eq roleCode)
                        select(table.fetch(AuthorizationFetchers.ROLE_WITH_MENUS))
                    }.execute()
                    .flatMap { role -> role.menus }
                    .map(::MenuView)
            }
        return menus
            .distinctBy { it.id }
            .sortedWith(compareBy<MenuView> { it.sortingOrder ?: Int.MAX_VALUE }.thenBy { it.id })
    }

    companion object {
        private val DEFAULT_SORT: Sort =
            Sort.by(Sort.Order.asc("sortingOrder"), Sort.Order.asc("id"))
    }
}
