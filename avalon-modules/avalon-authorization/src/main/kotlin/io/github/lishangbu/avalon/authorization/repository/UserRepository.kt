package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.*
import io.github.lishangbu.avalon.authorization.entity.dto.UserView
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.Specification
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.spring.repository.orderBy
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

/**
 * 用户仓储接口
 *
 * 定义用户数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2025/08/19
 */
interface UserRepository : KRepository<User, Long> {
    /** 按条件查询用户列表 */
    fun findAll(specification: Specification<User>?): List<User> =
        sql
            .createQuery(User::class) {
                specification?.let(::where)
                orderBy(DEFAULT_SORT)
                select(table.fetch(AuthorizationFetchers.USER))
            }.execute()

    /** 按条件分页查询用户 */
    fun findAll(
        specification: Specification<User>?,
        pageable: Pageable,
    ): Page<User> =
        sql
            .createQuery(User::class) {
                specification?.let(::where)
                orderBy(DEFAULT_SORT)
                select(table.fetch(AuthorizationFetchers.USER))
            }.fetchPage(pageable.pageNumber, pageable.pageSize)

    /** 按条件查询用户列表，并抓取角色 */
    fun listWithRoles(specification: Specification<User>?): List<User> =
        sql
            .createQuery(User::class) {
                specification?.let(::where)
                orderBy(DEFAULT_SORT)
                select(table.fetch(AuthorizationFetchers.USER_WITH_ROLES))
            }.execute()

    /** 按条件分页查询用户，并抓取角色 */
    fun pageWithRoles(
        specification: Specification<User>?,
        pageable: Pageable,
    ): Page<User> =
        sql
            .createQuery(User::class) {
                specification?.let(::where)
                orderBy(DEFAULT_SORT)
                select(table.fetch(AuthorizationFetchers.USER_WITH_ROLES))
            }.fetchPage(pageable.pageNumber, pageable.pageSize)

    /** 按条件分页查询用户视图 */
    fun pageViews(
        specification: Specification<User>?,
        pageable: Pageable,
    ): Page<UserView> =
        sql
            .createQuery(User::class) {
                specification?.let(::where)
                orderBy(DEFAULT_SORT)
                select(table.fetch(UserView::class))
            }.fetchPage(pageable.pageNumber, pageable.pageSize)

    /** 按条件查询用户视图列表 */
    fun listViews(specification: Specification<User>?): List<UserView> =
        sql
            .createQuery(User::class) {
                specification?.let(::where)
                orderBy(DEFAULT_SORT)
                select(table.fetch(UserView::class))
            }.execute()

    /** 按 ID 查询用户视图 */
    fun loadViewById(id: Long): UserView? =
        sql
            .createQuery(User::class) {
                where(table.id eq id)
                orderBy(DEFAULT_SORT)
                select(table.fetch(UserView::class))
            }.execute()
            .firstOrNull()

    /** 根据账号查找用户及角色列表 */
    fun loadByAccountWithRoles(account: String): User? {
        val found =
            loadByUsername(account)
                ?: loadByPhone(account)
                ?: loadByEmail(account)
                ?: return null

        val enabledRoles = found.roles.filter { it.enabled == true }
        if (enabledRoles.isEmpty()) {
            return null
        }
        return User(found) {
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
    }

    private fun loadByUsername(account: String): User? =
        sql
            .createQuery(User::class) {
                where(table.username eq account)
                orderBy(DEFAULT_SORT)
                select(table.fetch(AuthorizationFetchers.USER_WITH_ROLES))
            }.execute()
            .firstOrNull()

    private fun loadByPhone(account: String): User? =
        sql
            .createQuery(User::class) {
                where(table.phone eq account)
                orderBy(DEFAULT_SORT)
                select(table.fetch(AuthorizationFetchers.USER_WITH_ROLES))
            }.execute()
            .firstOrNull()

    private fun loadByEmail(account: String): User? =
        sql
            .createQuery(User::class) {
                where(table.email eq account)
                orderBy(DEFAULT_SORT)
                select(table.fetch(AuthorizationFetchers.USER_WITH_ROLES))
            }.execute()
            .firstOrNull()

    companion object {
        private val DEFAULT_SORT: Sort = Sort.by(Sort.Order.asc("id"))
    }
}
