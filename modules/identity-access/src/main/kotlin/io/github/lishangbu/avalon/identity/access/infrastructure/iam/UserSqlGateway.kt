package io.github.lishangbu.avalon.identity.access.infrastructure.iam

import io.github.lishangbu.avalon.identity.access.application.iam.query.UserPageQuery
import io.github.lishangbu.avalon.identity.access.domain.iam.*
import io.github.lishangbu.avalon.shared.application.query.Page
import io.github.lishangbu.avalon.shared.infra.mutiny.awaitSuspending
import io.github.lishangbu.avalon.shared.infra.sql.first
import io.github.lishangbu.avalon.shared.infra.sql.firstOrNull
import io.github.lishangbu.avalon.shared.infra.sql.toRows
import io.github.lishangbu.avalon.shared.infra.sql.withSuspendingTransaction
import io.vertx.mutiny.sqlclient.Pool
import io.vertx.mutiny.sqlclient.SqlConnection
import io.vertx.mutiny.sqlclient.Tuple
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

internal class UserSqlGateway(
    private val pool: Pool,
) {
    suspend fun pageUsers(query: UserPageQuery): Page<UserAccount> {
        val request = query.pageRequest
        val parameters = userFilterTuple(query).addInteger(request.size).addValue(request.offset)
        val totalItems =
            pool.preparedQuery(USER_COUNT_SQL)
                .execute(userFilterTuple(query))
                .awaitSuspending()
                .first()
                .getLong("total_items")

        val rows =
            pool.preparedQuery(USER_PAGE_SQL)
                .execute(parameters)
                .awaitSuspending()
                .toRows()

        val userIds = rows.map { it.getUUID("id") }
        val roleIdsByUser = loadUserRoleIdsByUserIds(userIds)
        val items =
            rows.map { row ->
                mapUser(row, roleIdsByUser[row.getUUID("id")] ?: emptySet())
            }

        return Page.of(
            items = items,
            request = request,
            totalItems = totalItems,
        )
    }

    suspend fun findUser(id: UserId): UserAccount? {
        val row =
            pool.preparedQuery("$USER_SELECT_SQL WHERE id = $1")
                .execute(Tuple.of(id.value))
                .awaitSuspending()
                .firstOrNull()
                ?: return null

        return mapUser(row, loadUserRoleIds(id.value))
    }

    suspend fun createUser(draft: UserAccountDraft): UserAccount =
        pool.withSuspendingTransaction { connection ->
            val passwordUpdatedAt = draft.passwordHash?.let { Instant.now().atOffset(ZoneOffset.UTC) }
            val row =
                connection.preparedQuery(
                    """
                    INSERT INTO iam.user_account (
                        username,
                        username_normalized,
                        phone,
                        phone_normalized,
                        email,
                        email_normalized,
                        avatar,
                        enabled,
                        password_hash,
                        password_updated_at
                    )
                    VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
                    RETURNING id
                    """.trimIndent(),
                ).execute(
                    Tuple.tuple()
                        .addString(draft.username)
                        .addString(normalizeUsername(draft.username))
                        .addValue(draft.phone)
                        .addValue(normalizePhone(draft.phone))
                        .addValue(draft.email)
                        .addValue(normalizeEmail(draft.email))
                        .addValue(draft.avatar)
                        .addBoolean(draft.enabled)
                        .addValue(draft.passwordHash)
                        .addValue(passwordUpdatedAt),
                ).awaitSuspending()
                    .first()

            val userId = row.getUUID("id")
            replaceUserRoles(connection, userId, draft.roleIds)
            requireUser(connection, userId)
        }

    suspend fun updateUser(
        id: UserId,
        draft: UserAccountDraft,
    ): UserAccount =
        pool.withSuspendingTransaction { connection ->
            val updatedCount =
                connection.preparedQuery(
                    """
                    UPDATE iam.user_account
                    SET username = $1,
                        username_normalized = $2,
                        phone = $3,
                        phone_normalized = $4,
                        email = $5,
                        email_normalized = $6,
                        avatar = $7,
                        enabled = $8,
                        password_hash = COALESCE($9, password_hash),
                        password_updated_at = CASE
                            WHEN $9 IS NOT NULL AND $9 IS DISTINCT FROM password_hash THEN CURRENT_TIMESTAMP
                            ELSE password_updated_at
                        END,
                        version = version + 1
                    WHERE id = $10
                    """.trimIndent(),
                ).execute(
                    Tuple.tuple()
                        .addString(draft.username)
                        .addString(normalizeUsername(draft.username))
                        .addValue(draft.phone)
                        .addValue(normalizePhone(draft.phone))
                        .addValue(draft.email)
                        .addValue(normalizeEmail(draft.email))
                        .addValue(draft.avatar)
                        .addBoolean(draft.enabled)
                        .addValue(draft.passwordHash)
                        .addValue(id.value),
                ).awaitSuspending()
                    .rowCount()

            if (updatedCount == 0) {
                throw IdentityAccessNotFound("user", id.value.toString())
            }

            replaceUserRoles(connection, id.value, draft.roleIds)
            requireUser(connection, id.value)
        }

    suspend fun deleteUser(id: UserId) {
        val deletedCount =
            pool.preparedQuery("DELETE FROM iam.user_account WHERE id = $1")
                .execute(Tuple.of(id.value))
                .awaitSuspending()
                .rowCount()

        if (deletedCount == 0) {
            throw IdentityAccessNotFound("user", id.value.toString())
        }
    }

    private suspend fun replaceUserRoles(
        connection: SqlConnection,
        userId: UUID,
        roleIds: Set<RoleId>,
    ) {
        // 当前聚合更新采用“全量替换连接表”策略，语义简单且能避免接口层传增量 patch 带来的状态分叉。
        connection.preparedQuery("DELETE FROM iam.user_role WHERE user_id = $1")
            .execute(Tuple.of(userId))
            .awaitSuspending()

        roleIds.forEach { roleId ->
            connection.preparedQuery("INSERT INTO iam.user_role (user_id, role_id) VALUES ($1, $2)")
                .execute(Tuple.of(userId, roleId.value))
                .awaitSuspending()
        }
    }

    private suspend fun requireUser(
        connection: SqlConnection,
        userId: UUID,
    ): UserAccount {
        val row =
            connection.preparedQuery("$USER_SELECT_SQL WHERE id = $1")
                .execute(Tuple.of(userId))
                .awaitSuspending()
                .first()

        return mapUser(row, loadUserRoleIds(connection, userId))
    }

    private suspend fun loadUserRoleIdsByUserIds(userIds: List<UUID>): Map<UUID, Set<RoleId>> {
        if (userIds.isEmpty()) {
            return emptyMap()
        }

        val placeholders = postgresParameterList(firstParameterIndex = 1, parameterCount = userIds.size)
        return pool.preparedQuery(
            """
            SELECT user_id, role_id
            FROM iam.user_role
            WHERE user_id IN ($placeholders)
            ORDER BY user_id, role_id
            """.trimIndent(),
        )
            .execute(tupleOfUuids(userIds))
            .awaitSuspending()
            .toRows()
            .groupBy(
                keySelector = { it.getUUID("user_id") },
                valueTransform = { RoleId(it.getUUID("role_id")) },
            ).mapValues { (_, roleIds) -> roleIds.toSet() }
    }

    private suspend fun loadUserRoleIds(userId: UUID): Set<RoleId> =
        pool.preparedQuery("SELECT role_id FROM iam.user_role WHERE user_id = $1 ORDER BY role_id")
            .execute(Tuple.of(userId))
            .awaitSuspending()
            .toRows()
            .mapTo(linkedSetOf()) { RoleId(it.getUUID("role_id")) }

    private suspend fun loadUserRoleIds(
        connection: SqlConnection,
        userId: UUID,
    ): Set<RoleId> =
        connection.preparedQuery("SELECT role_id FROM iam.user_role WHERE user_id = $1 ORDER BY role_id")
            .execute(Tuple.of(userId))
            .awaitSuspending()
            .toRows()
            .mapTo(linkedSetOf()) { RoleId(it.getUUID("role_id")) }

    private fun userFilterTuple(query: UserPageQuery): Tuple =
        Tuple.tuple()
            .addValue(query.id)
            .addValue(toContainsPattern(query.username?.let(::normalizeUsername)))
            .addValue(toContainsPattern(normalizePhone(query.phone)))
            .addValue(toContainsPattern(normalizeEmail(query.email)))
            .addValue(query.enabled)
}

