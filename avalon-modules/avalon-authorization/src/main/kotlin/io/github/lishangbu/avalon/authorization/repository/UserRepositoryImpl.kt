package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.*
import io.github.lishangbu.avalon.authorization.entity.dto.UserSpecification
import io.github.lishangbu.avalon.jimmer.support.readOrNull
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class UserRepositoryImpl(
    /** Jimmer SQL 客户端 */
    private val sql: KSqlClient,
) : UserRepository {
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

    /** 按 ID 查询用户 */
    override fun findById(id: Long): User? =
        sql
            .createQuery(User::class) {
                where(table.id eq id)
                select(table.fetch(AuthorizationFetchers.USER))
            }.execute()
            .firstOrNull()

    /** 按 ID 查询用户，并抓取角色 */
    override fun findByIdWithRoles(id: Long): User? =
        sql
            .createQuery(User::class) {
                where(table.id eq id)
                select(table.fetch(AuthorizationFetchers.USER_WITH_ROLES))
            }.execute()
            .firstOrNull()

    /** 保存用户 */
    override fun save(user: User): User =
        sql
            .save(user) {
                val mode = user.readOrNull { id }?.let { SaveMode.UPSERT } ?: SaveMode.INSERT_ONLY
                setMode(mode)
            }.modifiedEntity

    /** 保存用户并立即刷新 */
    override fun saveAndFlush(user: User): User = save(user)

    /** 按 ID 删除用户 */
    override fun deleteById(id: Long) {
        sql
            .createDelete(User::class) {
                where(table.id eq id)
                disableDissociation()
            }.execute()
    }

    /** 刷新持久化上下文 */
    override fun flush() = Unit

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
