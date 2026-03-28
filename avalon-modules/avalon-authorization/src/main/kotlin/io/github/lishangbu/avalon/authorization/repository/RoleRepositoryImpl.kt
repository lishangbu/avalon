package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.*
import io.github.lishangbu.avalon.authorization.entity.dto.RoleSpecification
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class RoleRepositoryImpl(
    /** Jimmer SQL 客户端 */
    private val sql: KSqlClient,
) : RoleRepositoryExt {
    /** 按条件查询角色列表 */
    override fun findAll(specification: RoleSpecification?): List<Role> =
        sql
            .createQuery(Role::class) {
                specification?.let { where(it) }
                select(table.fetch(AuthorizationFetchers.ROLE))
            }.execute()

    /** 按条件分页查询角色 */
    override fun findAll(
        specification: RoleSpecification?,
        pageable: Pageable,
    ): Page<Role> =
        sql
            .createQuery(Role::class) {
                specification?.let { where(it) }
                select(table.fetch(AuthorizationFetchers.ROLE))
            }.fetchPage(pageable.pageNumber, pageable.pageSize)

    /** 按条件查询角色列表，并抓取菜单 */
    override fun findAllWithMenus(specification: RoleSpecification?): List<Role> =
        sql
            .createQuery(Role::class) {
                specification?.let { where(it) }
                select(table.fetch(AuthorizationFetchers.ROLE_WITH_MENUS))
            }.execute()

    /** 按条件分页查询角色，并抓取菜单 */
    override fun findAllWithMenus(
        specification: RoleSpecification?,
        pageable: Pageable,
    ): Page<Role> =
        sql
            .createQuery(Role::class) {
                specification?.let { where(it) }
                select(table.fetch(AuthorizationFetchers.ROLE_WITH_MENUS))
            }.fetchPage(pageable.pageNumber, pageable.pageSize)

    /** 按 ID 删除角色 */
    override fun removeById(id: Long) {
        sql
            .createDelete(Role::class) {
                where(table.id eq id)
                disableDissociation()
            }.execute()
    }
}
