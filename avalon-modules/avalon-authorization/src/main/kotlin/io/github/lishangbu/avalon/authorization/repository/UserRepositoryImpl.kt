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

@Repository
class UserRepositoryImpl(
    /** Jimmer SQL 客户端 */
    private val sql: KSqlClient,
) : UserRepository {
    /** 按条件查询用户列表 */
    override fun findAll(example: Example<User>?): List<User> {
        val probe = example?.probe
        return sql
            .createQuery(User::class) {
                probe.readId()?.let { where(table.id eq it) }
                probe.readUsername().takeFilter()?.let { where(table.username ilike "%$it%") }
                probe.readPhone().takeFilter()?.let { where(table.phone ilike "%$it%") }
                probe.readEmail().takeFilter()?.let { where(table.email ilike "%$it%") }
                select(table.fetch(USER_WITH_ROLES_FETCHER))
            }.execute()
    }

    /** 按条件分页查询用户 */
    override fun findAll(
        example: Example<User>?,
        pageable: Pageable,
    ): Page<User> {
        val probe = example?.probe
        return sql
            .createQuery(User::class) {
                probe.readId()?.let { where(table.id eq it) }
                probe.readUsername().takeFilter()?.let { where(table.username ilike "%$it%") }
                probe.readPhone().takeFilter()?.let { where(table.phone ilike "%$it%") }
                probe.readEmail().takeFilter()?.let { where(table.email ilike "%$it%") }
                select(table.fetch(USER_WITH_ROLES_FETCHER))
            }.fetchPage(pageable.pageNumber, pageable.pageSize)
    }

    /** 按 ID 查询用户 */
    override fun findById(id: Long): User? =
        sql
            .createQuery(User::class) {
                where(table.id eq id)
                select(table.fetch(USER_WITH_ROLES_FETCHER))
            }.execute()
            .firstOrNull()

    /** 保存用户 */
    override fun save(user: User): User =
        sql
            .save(user) {
                val mode = user.readId()?.let { SaveMode.UPSERT } ?: SaveMode.INSERT_ONLY
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
    override fun findUserWithRolesByAccount(account: String): User? {
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
                select(table.fetch(USER_WITH_ROLES_FETCHER))
            }.execute()
            .firstOrNull()

    /** 根据手机查找用户 */
    private fun findByPhone(account: String): User? =
        sql
            .createQuery(User::class) {
                where(table.phone eq account)
                select(table.fetch(USER_WITH_ROLES_FETCHER))
            }.execute()
            .firstOrNull()

    /** 根据邮箱查找用户 */
    private fun findByEmail(account: String): User? =
        sql
            .createQuery(User::class) {
                where(table.email eq account)
                select(table.fetch(USER_WITH_ROLES_FETCHER))
            }.execute()
            .firstOrNull()

    companion object {
        /** 用户及角色列表抓取器 */
        private val USER_WITH_ROLES_FETCHER =
            newFetcher(User::class).`by` {
                allScalarFields()
                roles {
                    allScalarFields()
                }
            }
    }

    /** 安全读取主键 */
    private fun User?.readId(): Long? = readOrNull { id }

    /** 读取用户名 */
    private fun User?.readUsername(): String? = readOrNull { username }

    /** 读取手机 */
    private fun User?.readPhone(): String? = readOrNull { phone }

    /** 读取邮箱 */
    private fun User?.readEmail(): String? = readOrNull { email }
}
