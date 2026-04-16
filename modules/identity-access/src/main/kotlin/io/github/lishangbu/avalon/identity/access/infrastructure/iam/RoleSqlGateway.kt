package io.github.lishangbu.avalon.identity.access.infrastructure.iam

import io.github.lishangbu.avalon.identity.access.application.iam.query.RoleListQuery
import io.github.lishangbu.avalon.identity.access.application.iam.query.RolePageQuery
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
import java.util.UUID

internal class RoleSqlGateway(
    private val pool: Pool,
) {
    suspend fun pageRoles(query: RolePageQuery): Page<Role> {
        val request = query.pageRequest
        val parameters = roleFilterTuple(query).addInteger(request.size).addValue(request.offset)
        val totalItems =
            pool.preparedQuery(ROLE_COUNT_SQL)
                .execute(roleFilterTuple(query))
                .awaitSuspending()
                .first()
                .getLong("total_items")

        val rows =
            pool.preparedQuery(ROLE_PAGE_SQL)
                .execute(parameters)
                .awaitSuspending()
                .toRows()

        val roleIds = rows.map { it.getUUID("id") }
        val menuIdsByRole = loadRoleMenuIdsByRoleIds(roleIds)
        val permissionIdsByRole = loadRolePermissionIdsByRoleIds(roleIds)
        val items =
            rows.map { row ->
                mapRole(
                    row = row,
                    menuIds = menuIdsByRole[row.getUUID("id")] ?: emptySet(),
                    permissionIds = permissionIdsByRole[row.getUUID("id")] ?: emptySet(),
                )
            }

        return Page.of(
            items = items,
            request = request,
            totalItems = totalItems,
        )
    }

    suspend fun listRoles(query: RoleListQuery): List<Role> {
        val rows =
            pool.preparedQuery(ROLE_LIST_SQL)
                .execute(roleFilterTuple(query))
                .awaitSuspending()
                .toRows()

        val roleIds = rows.map { it.getUUID("id") }
        val menuIdsByRole = loadRoleMenuIdsByRoleIds(roleIds)
        val permissionIdsByRole = loadRolePermissionIdsByRoleIds(roleIds)
        return rows.map { row ->
            mapRole(
                row = row,
                menuIds = menuIdsByRole[row.getUUID("id")] ?: emptySet(),
                permissionIds = permissionIdsByRole[row.getUUID("id")] ?: emptySet(),
            )
        }
    }

    suspend fun findRole(id: RoleId): Role? {
        val row =
            pool.preparedQuery("$ROLE_SELECT_SQL WHERE id = $1")
                .execute(Tuple.of(id.value))
                .awaitSuspending()
                .firstOrNull()
                ?: return null

        return mapRole(
            row = row,
            menuIds = loadRoleMenuIds(id.value),
            permissionIds = loadRolePermissionIds(id.value),
        )
    }

    suspend fun createRole(draft: RoleDraft): Role =
        pool.withSuspendingTransaction { connection ->
            val row =
                connection.preparedQuery(
                    """
                    INSERT INTO iam.role (
                        code,
                        name,
                        enabled
                    )
                    VALUES ($1, $2, $3)
                    RETURNING id
                    """.trimIndent(),
                ).execute(Tuple.of(draft.code, draft.name, draft.enabled))
                    .awaitSuspending()
                    .first()

            val roleId = row.getUUID("id")
            replaceRoleMenus(connection, roleId, draft.menuIds)
            replaceRolePermissions(connection, roleId, draft.permissionIds)
            requireRole(connection, roleId)
        }

    suspend fun updateRole(
        id: RoleId,
        draft: RoleDraft,
    ): Role =
        pool.withSuspendingTransaction { connection ->
            val updatedCount =
                connection.preparedQuery(
                    """
                    UPDATE iam.role
                    SET code = $1,
                        name = $2,
                        enabled = $3,
                        version = version + 1
                    WHERE id = $4
                    """.trimIndent(),
                ).execute(Tuple.of(draft.code, draft.name, draft.enabled, id.value))
                    .awaitSuspending()
                    .rowCount()

            if (updatedCount == 0) {
                throw IdentityAccessNotFound("role", id.value.toString())
            }

            replaceRoleMenus(connection, id.value, draft.menuIds)
            replaceRolePermissions(connection, id.value, draft.permissionIds)
            requireRole(connection, id.value)
        }

    suspend fun deleteRole(id: RoleId) {
        val deletedCount =
            pool.preparedQuery("DELETE FROM iam.role WHERE id = $1")
                .execute(Tuple.of(id.value))
                .awaitSuspending()
                .rowCount()

        if (deletedCount == 0) {
            throw IdentityAccessNotFound("role", id.value.toString())
        }
    }

    internal suspend fun loadRolesByUser(userId: UUID): List<Role> {
        val menuIdsByRole = loadAllRoleMenuIds()
        val permissionIdsByRole = loadAllRolePermissionIds()
        return pool.preparedQuery(
            """
            $ROLE_SELECT_SQL
            INNER JOIN iam.user_role ur ON ur.role_id = id
            WHERE ur.user_id = $1
            ORDER BY code, id
            """.trimIndent(),
        )
            .execute(Tuple.of(userId))
            .awaitSuspending()
            .toRows()
            .map { row ->
                mapRole(
                    row = row,
                    menuIds = menuIdsByRole[row.getUUID("id")] ?: emptySet(),
                    permissionIds = permissionIdsByRole[row.getUUID("id")] ?: emptySet(),
                )
            }
    }

    private suspend fun replaceRoleMenus(
        connection: SqlConnection,
        roleId: UUID,
        menuIds: Set<MenuId>,
    ) {
        connection.preparedQuery("DELETE FROM iam.role_menu WHERE role_id = $1")
            .execute(Tuple.of(roleId))
            .awaitSuspending()

        menuIds.forEach { menuId ->
            connection.preparedQuery("INSERT INTO iam.role_menu (role_id, menu_id) VALUES ($1, $2)")
                .execute(Tuple.of(roleId, menuId.value))
                .awaitSuspending()
        }
    }

    private suspend fun replaceRolePermissions(
        connection: SqlConnection,
        roleId: UUID,
        permissionIds: Set<PermissionId>,
    ) {
        connection.preparedQuery("DELETE FROM iam.role_permission WHERE role_id = $1")
            .execute(Tuple.of(roleId))
            .awaitSuspending()

        permissionIds.forEach { permissionId ->
            connection.preparedQuery("INSERT INTO iam.role_permission (role_id, permission_id) VALUES ($1, $2)")
                .execute(Tuple.of(roleId, permissionId.value))
                .awaitSuspending()
        }
    }

    private suspend fun requireRole(
        connection: SqlConnection,
        roleId: UUID,
    ): Role {
        val row =
            connection.preparedQuery("$ROLE_SELECT_SQL WHERE id = $1")
                .execute(Tuple.of(roleId))
                .awaitSuspending()
                .first()

        return mapRole(
            row = row,
            menuIds = loadRoleMenuIds(connection, roleId),
            permissionIds = loadRolePermissionIds(connection, roleId),
        )
    }

    private suspend fun loadAllRoleMenuIds(): Map<UUID, Set<MenuId>> =
        pool.query("SELECT role_id, menu_id FROM iam.role_menu ORDER BY role_id, menu_id")
            .execute()
            .awaitSuspending()
            .toRows()
            .groupBy(
                keySelector = { it.getUUID("role_id") },
                valueTransform = { MenuId(it.getUUID("menu_id")) },
            ).mapValues { (_, menuIds) -> menuIds.toSet() }

    private suspend fun loadRoleMenuIdsByRoleIds(roleIds: List<UUID>): Map<UUID, Set<MenuId>> {
        if (roleIds.isEmpty()) {
            return emptyMap()
        }

        val placeholders = postgresParameterList(firstParameterIndex = 1, parameterCount = roleIds.size)
        return pool.preparedQuery(
            """
            SELECT role_id, menu_id
            FROM iam.role_menu
            WHERE role_id IN ($placeholders)
            ORDER BY role_id, menu_id
            """.trimIndent(),
        )
            .execute(tupleOfUuids(roleIds))
            .awaitSuspending()
            .toRows()
            .groupBy(
                keySelector = { it.getUUID("role_id") },
                valueTransform = { MenuId(it.getUUID("menu_id")) },
            ).mapValues { (_, menuIds) -> menuIds.toSet() }
    }

    private suspend fun loadRoleMenuIds(roleId: UUID): Set<MenuId> =
        pool.preparedQuery("SELECT menu_id FROM iam.role_menu WHERE role_id = $1 ORDER BY menu_id")
            .execute(Tuple.of(roleId))
            .awaitSuspending()
            .toRows()
            .mapTo(linkedSetOf()) { MenuId(it.getUUID("menu_id")) }

    private suspend fun loadRoleMenuIds(
        connection: SqlConnection,
        roleId: UUID,
    ): Set<MenuId> =
        connection.preparedQuery("SELECT menu_id FROM iam.role_menu WHERE role_id = $1 ORDER BY menu_id")
            .execute(Tuple.of(roleId))
            .awaitSuspending()
            .toRows()
            .mapTo(linkedSetOf()) { MenuId(it.getUUID("menu_id")) }

    private suspend fun loadAllRolePermissionIds(): Map<UUID, Set<PermissionId>> =
        pool.query("SELECT role_id, permission_id FROM iam.role_permission ORDER BY role_id, permission_id")
            .execute()
            .awaitSuspending()
            .toRows()
            .groupBy(
                keySelector = { it.getUUID("role_id") },
                valueTransform = { PermissionId(it.getUUID("permission_id")) },
            ).mapValues { (_, permissionIds) -> permissionIds.toSet() }

    private suspend fun loadRolePermissionIdsByRoleIds(roleIds: List<UUID>): Map<UUID, Set<PermissionId>> {
        if (roleIds.isEmpty()) {
            return emptyMap()
        }

        val placeholders = postgresParameterList(firstParameterIndex = 1, parameterCount = roleIds.size)
        return pool.preparedQuery(
            """
            SELECT role_id, permission_id
            FROM iam.role_permission
            WHERE role_id IN ($placeholders)
            ORDER BY role_id, permission_id
            """.trimIndent(),
        )
            .execute(tupleOfUuids(roleIds))
            .awaitSuspending()
            .toRows()
            .groupBy(
                keySelector = { it.getUUID("role_id") },
                valueTransform = { PermissionId(it.getUUID("permission_id")) },
            ).mapValues { (_, permissionIds) -> permissionIds.toSet() }
    }

    private suspend fun loadRolePermissionIds(roleId: UUID): Set<PermissionId> =
        pool.preparedQuery("SELECT permission_id FROM iam.role_permission WHERE role_id = $1 ORDER BY permission_id")
            .execute(Tuple.of(roleId))
            .awaitSuspending()
            .toRows()
            .mapTo(linkedSetOf()) { PermissionId(it.getUUID("permission_id")) }

    private suspend fun loadRolePermissionIds(
        connection: SqlConnection,
        roleId: UUID,
    ): Set<PermissionId> =
        connection.preparedQuery("SELECT permission_id FROM iam.role_permission WHERE role_id = $1 ORDER BY permission_id")
            .execute(Tuple.of(roleId))
            .awaitSuspending()
            .toRows()
            .mapTo(linkedSetOf()) { PermissionId(it.getUUID("permission_id")) }

    private fun roleFilterTuple(query: RolePageQuery): Tuple =
        Tuple.tuple()
            .addValue(query.id)
            .addValue(toContainsPattern(query.code))
            .addValue(toContainsPattern(query.name))
            .addValue(query.enabled)

    private fun roleFilterTuple(query: RoleListQuery): Tuple =
        Tuple.tuple()
            .addValue(query.id)
            .addValue(toContainsPattern(query.code))
            .addValue(toContainsPattern(query.name))
            .addValue(query.enabled)
}

