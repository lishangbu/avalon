package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.*
import io.github.lishangbu.avalon.authorization.entity.dto.RoleSpecification
import io.github.lishangbu.avalon.jimmer.support.readOrNull
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class RoleRepositoryImpl(
    /** Jimmer SQL 客户端 */
    private val sql: KSqlClient,
) : RoleRepository {
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

    /** 按 ID 查询角色 */
    override fun findById(id: Long): Role? =
        sql
            .createQuery(Role::class) {
                where(table.id eq id)
                select(table.fetch(AuthorizationFetchers.ROLE))
            }.execute()
            .firstOrNull()

    /** 按 ID 查询角色，并抓取菜单 */
    override fun findByIdWithMenus(id: Long): Role? =
        sql
            .createQuery(Role::class) {
                where(table.id eq id)
                select(table.fetch(AuthorizationFetchers.ROLE_WITH_MENUS))
            }.execute()
            .firstOrNull()

    /** 按 ID 列表查询角色 */
    override fun findAllById(ids: Iterable<Long>): List<Role> = ids.mapNotNull(::findById)

    /** 保存角色 */
    override fun save(role: Role): Role =
        sql
            .save(role) {
                val mode = role.readOrNull { id }?.let { SaveMode.UPSERT } ?: SaveMode.INSERT_ONLY
                setMode(mode)
            }.modifiedEntity

    /** 保存角色并立即刷新 */
    override fun saveAndFlush(role: Role): Role = save(role)

    /** 按 ID 删除角色 */
    override fun deleteById(id: Long) {
        sql
            .createDelete(Role::class) {
                where(table.id eq id)
                disableDissociation()
            }.execute()
    }

    /** 刷新持久化上下文 */
    override fun flush() = Unit
}
