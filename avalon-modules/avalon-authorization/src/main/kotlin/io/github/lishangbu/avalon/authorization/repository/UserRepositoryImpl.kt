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
    private val sql: KSqlClient,
) : UserRepository {
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

    override fun findById(id: Long): User? =
        sql
            .createQuery(User::class) {
                where(table.id eq id)
                select(table.fetch(USER_WITH_ROLES_FETCHER))
            }.execute()
            .firstOrNull()

    override fun save(user: User): User =
        sql
            .save(user) {
                val mode = user.readId()?.let { SaveMode.UPSERT } ?: SaveMode.INSERT_ONLY
                setMode(mode)
            }.modifiedEntity

    override fun saveAndFlush(user: User): User = save(user)

    override fun deleteById(id: Long) {
        sql
            .createDelete(User::class) {
                where(table.id eq id)
                disableDissociation()
            }.execute()
    }

    override fun flush() = Unit

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

    private fun findByUsername(account: String): User? =
        sql
            .createQuery(User::class) {
                where(table.username eq account)
                select(table.fetch(USER_WITH_ROLES_FETCHER))
            }.execute()
            .firstOrNull()

    private fun findByPhone(account: String): User? =
        sql
            .createQuery(User::class) {
                where(table.phone eq account)
                select(table.fetch(USER_WITH_ROLES_FETCHER))
            }.execute()
            .firstOrNull()

    private fun findByEmail(account: String): User? =
        sql
            .createQuery(User::class) {
                where(table.email eq account)
                select(table.fetch(USER_WITH_ROLES_FETCHER))
            }.execute()
            .firstOrNull()

    companion object {
        private val USER_WITH_ROLES_FETCHER =
            newFetcher(User::class).`by` {
                allScalarFields()
                roles {
                    allScalarFields()
                }
            }
    }

    private fun User?.readId(): Long? = readOrNull { id }

    private fun User?.readUsername(): String? = readOrNull { username }

    private fun User?.readPhone(): String? = readOrNull { phone }

    private fun User?.readEmail(): String? = readOrNull { email }
}
