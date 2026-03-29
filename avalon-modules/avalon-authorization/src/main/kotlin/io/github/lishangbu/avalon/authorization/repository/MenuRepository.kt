package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.*
import org.babyfish.jimmer.Specification
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.spring.repository.orderBy
import org.babyfish.jimmer.sql.kt.ast.expression.eq
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

    /** 按条件排序查询菜单列表 */
    fun findAll(
        specification: Specification<Menu>?,
        sort: Sort,
    ): List<Menu> =
        sql
            .createQuery(Menu::class) {
                specification?.let(::where)
                orderBy(sort)
                select(table.fetch(AuthorizationFetchers.MENU))
            }.execute()

    /** 按排序顺序和 ID 升序查询菜单列表 */
    fun findAllByOrderBySortingOrderAscIdAsc(): List<Menu>

    /** 按角色编码列表查询菜单 */
    fun listByRoleCodes(roleCodes: List<String>): List<Menu> {
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
                    .flatMap { it.menus }
            }
        return menus
            .distinctBy { it.id }
            .sortedWith(compareBy<Menu> { it.sortingOrder ?: Int.MAX_VALUE }.thenBy { it.id })
    }
}
