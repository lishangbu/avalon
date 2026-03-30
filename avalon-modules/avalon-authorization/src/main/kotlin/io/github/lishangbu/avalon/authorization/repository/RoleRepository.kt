package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.*
import io.github.lishangbu.avalon.authorization.entity.dto.RoleView
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.Specification
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Pageable

/**
 * 角色仓储接口
 *
 * 定义角色数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2025/08/20
 */
interface RoleRepository : KRepository<Role, Long> {
    /** 按条件查询角色列表 */
    fun findAll(specification: Specification<Role>?): List<Role> =
        sql
            .createQuery(Role::class) {
                specification?.let(::where)
                select(table.fetch(AuthorizationFetchers.ROLE))
            }.execute()

    /** 按条件分页查询角色 */
    fun findAll(
        specification: Specification<Role>?,
        pageable: Pageable,
    ): Page<Role> =
        sql
            .createQuery(Role::class) {
                specification?.let(::where)
                select(table.fetch(AuthorizationFetchers.ROLE))
            }.fetchPage(pageable.pageNumber, pageable.pageSize)

    /** 按条件查询角色列表，并抓取菜单 */
    fun listWithMenus(specification: Specification<Role>?): List<Role> =
        sql
            .createQuery(Role::class) {
                specification?.let(::where)
                select(table.fetch(AuthorizationFetchers.ROLE_WITH_MENUS))
            }.execute()

    /** 按条件分页查询角色，并抓取菜单 */
    fun pageWithMenus(
        specification: Specification<Role>?,
        pageable: Pageable,
    ): Page<Role> =
        sql
            .createQuery(Role::class) {
                specification?.let(::where)
                select(table.fetch(AuthorizationFetchers.ROLE_WITH_MENUS))
            }.fetchPage(pageable.pageNumber, pageable.pageSize)

    /** 按条件分页查询角色视图 */
    fun pageViews(
        specification: Specification<Role>?,
        pageable: Pageable,
    ): Page<RoleView> =
        sql
            .createQuery(Role::class) {
                specification?.let(::where)
                select(table.fetch(RoleView::class))
            }.fetchPage(pageable.pageNumber, pageable.pageSize)

    /** 按条件查询角色视图列表 */
    fun listViews(specification: Specification<Role>?): List<RoleView> =
        sql
            .createQuery(Role::class) {
                specification?.let(::where)
                select(table.fetch(RoleView::class))
            }.execute()

    /** 按 ID 查询角色视图 */
    fun loadViewById(id: Long): RoleView? =
        sql
            .createQuery(Role::class) {
                where(table.id eq id)
                select(table.fetch(RoleView::class))
            }.execute()
            .firstOrNull()
}
