package io.github.lishangbu.avalon.identity.access.infrastructure.iam

import io.github.lishangbu.avalon.identity.access.application.iam.query.PermissionListQuery
import io.github.lishangbu.avalon.identity.access.application.iam.query.PermissionPageQuery
import io.github.lishangbu.avalon.identity.access.domain.iam.IdentityAccessNotFound
import io.github.lishangbu.avalon.identity.access.domain.iam.Permission
import io.github.lishangbu.avalon.identity.access.domain.iam.PermissionDraft
import io.github.lishangbu.avalon.identity.access.domain.iam.PermissionId
import io.github.lishangbu.avalon.shared.application.query.Page
import io.github.lishangbu.avalon.shared.infra.mutiny.awaitSuspending
import io.github.lishangbu.avalon.shared.infra.sql.first
import io.github.lishangbu.avalon.shared.infra.sql.firstOrNull
import io.github.lishangbu.avalon.shared.infra.sql.toRows
import io.github.lishangbu.avalon.shared.infra.sql.withSuspendingTransaction
import io.vertx.mutiny.sqlclient.Pool
import io.vertx.mutiny.sqlclient.SqlConnection
import io.vertx.mutiny.sqlclient.Tuple
import java.util.UUID

internal class PermissionSqlGateway(
    private val pool: Pool,
) {
    suspend fun pagePermissions(query: PermissionPageQuery): Page<Permission> {
        val request = query.pageRequest
        val parameters = permissionFilterTuple(query).addInteger(request.size).addValue(request.offset)
        val totalItems =
            pool.preparedQuery(PERMISSION_COUNT_SQL)
                .execute(permissionFilterTuple(query))
                .awaitSuspending()
                .first()
                .getLong("total_items")

        val items =
            pool.preparedQuery(PERMISSION_PAGE_SQL)
                .execute(parameters)
                .awaitSuspending()
                .toRows()
                .map(::mapPermission)

        return Page.of(
            items = items,
            request = request,
            totalItems = totalItems,
        )
    }

    suspend fun listPermissions(query: PermissionListQuery): List<Permission> =
        pool.preparedQuery(PERMISSION_LIST_SQL)
            .execute(permissionFilterTuple(query))
            .awaitSuspending()
            .toRows()
            .map(::mapPermission)

    suspend fun findPermission(id: PermissionId): Permission? =
        pool.preparedQuery("$PERMISSION_SELECT_SQL WHERE id = $1")
            .execute(Tuple.of(id.value))
            .awaitSuspending()
            .firstOrNull()
            ?.let(::mapPermission)

    suspend fun createPermission(draft: PermissionDraft): Permission =
        pool.withSuspendingTransaction { connection ->
            val row =
                connection.preparedQuery(
                    """
                    INSERT INTO iam.permission (
                        menu_id,
                        code,
                        name,
                        enabled,
                        sorting_order
                    )
                    VALUES ($1, $2, $3, $4, $5)
                    RETURNING id
                    """.trimIndent(),
                ).execute(
                    Tuple.of(
                        draft.menuId.value,
                        draft.code,
                        draft.name,
                        draft.enabled,
                        draft.sortingOrder,
                    ),
                ).awaitSuspending()
                    .first()

            requirePermission(connection, row.getUUID("id"))
        }

    suspend fun updatePermission(
        id: PermissionId,
        draft: PermissionDraft,
    ): Permission =
        pool.withSuspendingTransaction { connection ->
            val updatedCount =
                connection.preparedQuery(
                    """
                    UPDATE iam.permission
                    SET menu_id = $1,
                        code = $2,
                        name = $3,
                        enabled = $4,
                        sorting_order = $5,
                        version = version + 1
                    WHERE id = $6
                    """.trimIndent(),
                ).execute(
                    Tuple.of(
                        draft.menuId.value,
                        draft.code,
                        draft.name,
                        draft.enabled,
                        draft.sortingOrder,
                        id.value,
                    ),
                ).awaitSuspending()
                    .rowCount()

            if (updatedCount == 0) {
                throw IdentityAccessNotFound("permission", id.value.toString())
            }

            requirePermission(connection, id.value)
        }

    suspend fun deletePermission(id: PermissionId) {
        val deletedCount =
            pool.preparedQuery("DELETE FROM iam.permission WHERE id = $1")
                .execute(Tuple.of(id.value))
                .awaitSuspending()
                .rowCount()

        if (deletedCount == 0) {
            throw IdentityAccessNotFound("permission", id.value.toString())
        }
    }

    internal suspend fun loadPermissionsByUser(userId: UUID): List<Permission> =
        pool.preparedQuery(
            """
            SELECT DISTINCT
                p.id,
                p.menu_id,
                p.code,
                p.name,
                p.enabled,
                p.sorting_order,
                p.version
            FROM iam.permission p
            INNER JOIN iam.role_permission rp ON rp.permission_id = p.id
            INNER JOIN iam.user_role ur ON ur.role_id = rp.role_id
            WHERE ur.user_id = $1
            ORDER BY p.sorting_order, p.id
            """.trimIndent(),
        )
            .execute(Tuple.of(userId))
            .awaitSuspending()
            .toRows()
            .map(::mapPermission)

    private suspend fun requirePermission(
        connection: SqlConnection,
        permissionId: UUID,
    ): Permission =
        connection.preparedQuery("$PERMISSION_SELECT_SQL WHERE id = $1")
            .execute(Tuple.of(permissionId))
            .awaitSuspending()
            .first()
            .let(::mapPermission)

    private fun permissionFilterTuple(query: PermissionPageQuery): Tuple =
        Tuple.tuple()
            .addValue(query.id)
            .addValue(query.menuId)
            .addValue(toContainsPattern(query.code))
            .addValue(toContainsPattern(query.name))
            .addValue(query.enabled)

    private fun permissionFilterTuple(query: PermissionListQuery): Tuple =
        Tuple.tuple()
            .addValue(query.id)
            .addValue(query.menuId)
            .addValue(toContainsPattern(query.code))
            .addValue(toContainsPattern(query.name))
            .addValue(query.enabled)
}

