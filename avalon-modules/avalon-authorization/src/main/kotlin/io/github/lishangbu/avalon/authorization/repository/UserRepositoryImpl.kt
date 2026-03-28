package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.*
import io.github.lishangbu.avalon.authorization.entity.dto.UserSpecification
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class UserRepositoryImpl(
    /** Jimmer SQL 客户端 */
    private val sql: KSqlClient,
) : UserRepositoryExt {
    /** 按条件查询用户列表 */
    override fun findAll(specification: UserSpecification?): List<User> =
        sql
            .createQuery(User::class) {
                specification?.let { where(it) }
                select(table.fetch(AuthorizationFetchers.USER))
            }.execute()

    /** 按条件分页查询用户 */
    override fun findAll(
        specification: UserSpecification?,
        pageable: Pageable,
    ): Page<User> =
        sql
            .createQuery(User::class) {
                specification?.let { where(it) }
                select(table.fetch(AuthorizationFetchers.USER))
            }.fetchPage(pageable.pageNumber, pageable.pageSize)

    /** 按条件查询用户列表，并抓取角色 */
    override fun findAllWithRoles(specification: UserSpecification?): List<User> =
        sql
            .createQuery(User::class) {
                specification?.let { where(it) }
                select(table.fetch(AuthorizationFetchers.USER_WITH_ROLES))
            }.execute()

    /** 按条件分页查询用户，并抓取角色 */
    override fun findAllWithRoles(
        specification: UserSpecification?,
        pageable: Pageable,
    ): Page<User> =
        sql
            .createQuery(User::class) {
                specification?.let { where(it) }
                select(table.fetch(AuthorizationFetchers.USER_WITH_ROLES))
            }.fetchPage(pageable.pageNumber, pageable.pageSize)

    /** 按 ID 删除用户 */
    override fun removeById(id: Long) {
        sql
            .createDelete(User::class) {
                where(table.id eq id)
                disableDissociation()
            }.execute()
    }

    /** 根据账号查找用户及角色列表 */
    override fun findByAccountWithRoles(account: String): User? {
        val found =
            findByUsername(account)
                ?: findByPhone(account)
                ?: findByEmail(account)
                ?: return null

        val enabledRoles = found.roles.filter { it.enabled == true }
        if (enabledRoles.isEmpty()) {
            return null
        }
        val userWithEnabledRoles =
            User(found) {
                roles().clear()
                enabledRoles.forEach { role ->
                    roles().addBy {
                        id = role.id
                        code = role.code
                        name = role.name
                        enabled = role.enabled
                    }
                }
            }
        return userWithEnabledRoles
    }

    /** 根据用户名查找用户 */
    private fun findByUsername(account: String): User? =
        sql
            .createQuery(User::class) {
                where(table.username eq account)
                select(table.fetch(AuthorizationFetchers.USER_WITH_ROLES))
            }.execute()
            .firstOrNull()

    /** 根据手机查找用户 */
    private fun findByPhone(account: String): User? =
        sql
            .createQuery(User::class) {
                where(table.phone eq account)
                select(table.fetch(AuthorizationFetchers.USER_WITH_ROLES))
            }.execute()
            .firstOrNull()

    /** 根据邮箱查找用户 */
    private fun findByEmail(account: String): User? =
        sql
            .createQuery(User::class) {
                where(table.email eq account)
                select(table.fetch(AuthorizationFetchers.USER_WITH_ROLES))
            }.execute()
            .firstOrNull()
}
