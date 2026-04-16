package io.github.lishangbu.avalon.identity.access.infrastructure.iam

import io.github.lishangbu.avalon.identity.access.domain.iam.IdentityAccessNotFound
import io.github.lishangbu.avalon.identity.access.domain.iam.Menu
import io.github.lishangbu.avalon.identity.access.domain.iam.MenuDraft
import io.github.lishangbu.avalon.identity.access.domain.iam.MenuId
import io.github.lishangbu.avalon.shared.application.query.Page
import io.github.lishangbu.avalon.shared.application.query.PageRequest
import io.github.lishangbu.avalon.shared.infra.mutiny.awaitSuspending
import io.github.lishangbu.avalon.shared.infra.sql.first
import io.github.lishangbu.avalon.shared.infra.sql.firstOrNull
import io.github.lishangbu.avalon.shared.infra.sql.pagination.addLimitThenOffset
import io.github.lishangbu.avalon.shared.infra.sql.pagination.PostgresPaginationDialect
import io.github.lishangbu.avalon.shared.infra.sql.toRows
import io.github.lishangbu.avalon.shared.infra.sql.withSuspendingTransaction
import io.vertx.mutiny.sqlclient.Pool
import io.vertx.mutiny.sqlclient.SqlConnection
import io.vertx.mutiny.sqlclient.Tuple
import java.util.UUID

internal class MenuSqlGateway(
    private val pool: Pool,
) {
    private val pageMenusSql =
        buildString {
            append(MENU_SELECT_SQL)
            appendLine()
            append(MENU_DEFAULT_ORDER_BY_SQL)
            appendLine()
            append(PostgresPaginationDialect.renderLimitOffsetClause(firstParameterIndex = 1))
        }

    suspend fun listMenus(): List<Menu> =
        pool.query("$MENU_SELECT_SQL $MENU_DEFAULT_ORDER_BY_SQL")
            .execute()
            .awaitSuspending()
            .toRows()
            .map(::mapMenu)

    suspend fun pageMenus(request: PageRequest): Page<Menu> {
        val totalItems =
            pool.query(MENU_COUNT_SQL)
                .execute()
                .awaitSuspending()
                .first()
                .getLong("total_items")

        val items =
            pool.preparedQuery(pageMenusSql)
                .execute(Tuple.tuple().addLimitThenOffset(request))
                .awaitSuspending()
                .toRows()
                .map(::mapMenu)

        return Page.of(
            items = items,
            request = request,
            totalItems = totalItems,
        )
    }

    suspend fun findMenu(id: MenuId): Menu? =
        pool.preparedQuery("$MENU_SELECT_SQL WHERE id = $1")
            .execute(Tuple.of(id.value))
            .awaitSuspending()
            .firstOrNull()
            ?.let(::mapMenu)

    suspend fun createMenu(draft: MenuDraft): Menu =
        pool.withSuspendingTransaction { connection ->
            val row =
                connection.preparedQuery(
                    """
                    INSERT INTO iam.menu (
                        parent_id,
                        disabled,
                        extra,
                        icon,
                        menu_key,
                        title,
                        visible,
                        path,
                        route_name,
                        redirect,
                        component,
                        sorting_order,
                        pinned,
                        show_tab,
                        enable_multi_tab,
                        menu_type,
                        hidden,
                        hide_children_in_menu,
                        flat_menu,
                        active_menu,
                        external,
                        target
                    )
                    VALUES (
                        $1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11,
                        $12, $13, $14, $15, $16, $17, $18, $19, $20, $21, $22
                    )
                    RETURNING id
                    """.trimIndent(),
                ).execute(menuTuple(draft))
                    .awaitSuspending()
                    .first()

            requireMenu(connection, row.getUUID("id"))
        }

    suspend fun updateMenu(
        id: MenuId,
        draft: MenuDraft,
    ): Menu =
        pool.withSuspendingTransaction { connection ->
            val updatedCount =
                connection.preparedQuery(
                    """
                    UPDATE iam.menu
                    SET parent_id = $1,
                        disabled = $2,
                        extra = $3,
                        icon = $4,
                        menu_key = $5,
                        title = $6,
                        visible = $7,
                        path = $8,
                        route_name = $9,
                        redirect = $10,
                        component = $11,
                        sorting_order = $12,
                        pinned = $13,
                        show_tab = $14,
                        enable_multi_tab = $15,
                        menu_type = $16,
                        hidden = $17,
                        hide_children_in_menu = $18,
                        flat_menu = $19,
                        active_menu = $20,
                        external = $21,
                        target = $22,
                        version = version + 1
                    WHERE id = $23
                    """.trimIndent(),
                ).execute(menuTuple(draft).addValue(id.value))
                    .awaitSuspending()
                    .rowCount()

            if (updatedCount == 0) {
                throw IdentityAccessNotFound("menu", id.value.toString())
            }

            requireMenu(connection, id.value)
        }

    suspend fun deleteMenu(id: MenuId) {
        val deletedCount =
            pool.preparedQuery("DELETE FROM iam.menu WHERE id = $1")
                .execute(Tuple.of(id.value))
                .awaitSuspending()
                .rowCount()

        if (deletedCount == 0) {
            throw IdentityAccessNotFound("menu", id.value.toString())
        }
    }

    internal suspend fun loadAuthorizedMenus(userId: UUID): List<Menu> =
        pool.preparedQuery(
            """
            -- 先取角色直接授权的菜单，再递归补齐所有祖先目录，保证前端拿到的 menu tree 始终完整可构造。
            WITH RECURSIVE authorized_menu AS (
                SELECT DISTINCT
                    m.id,
                    m.parent_id,
                    m.disabled,
                    m.extra,
                    m.icon,
                    m.menu_key,
                    m.title,
                    m.visible,
                    m.path,
                    m.route_name,
                    m.redirect,
                    m.component,
                    m.sorting_order,
                    m.pinned,
                    m.show_tab,
                    m.enable_multi_tab,
                    m.menu_type,
                    m.hidden,
                    m.hide_children_in_menu,
                    m.flat_menu,
                    m.active_menu,
                    m.external,
                    m.target,
                    m.version
                FROM iam.menu m
                INNER JOIN iam.role_menu rm ON rm.menu_id = m.id
                INNER JOIN iam.user_role ur ON ur.role_id = rm.role_id
                WHERE ur.user_id = $1

                UNION

                SELECT
                    parent.id,
                    parent.parent_id,
                    parent.disabled,
                    parent.extra,
                    parent.icon,
                    parent.menu_key,
                    parent.title,
                    parent.visible,
                    parent.path,
                    parent.route_name,
                    parent.redirect,
                    parent.component,
                    parent.sorting_order,
                    parent.pinned,
                    parent.show_tab,
                    parent.enable_multi_tab,
                    parent.menu_type,
                    parent.hidden,
                    parent.hide_children_in_menu,
                    parent.flat_menu,
                    parent.active_menu,
                    parent.external,
                    parent.target,
                    parent.version
                FROM iam.menu parent
                INNER JOIN authorized_menu child ON child.parent_id = parent.id
            )
            SELECT DISTINCT
                id,
                parent_id,
                disabled,
                extra,
                icon,
                menu_key,
                title,
                visible,
                path,
                route_name,
                redirect,
                component,
                sorting_order,
                pinned,
                show_tab,
                enable_multi_tab,
                menu_type,
                hidden,
                hide_children_in_menu,
                flat_menu,
                active_menu,
                external,
                target,
                version
            FROM authorized_menu
            ORDER BY sorting_order, id
            """.trimIndent(),
        )
            .execute(Tuple.of(userId))
            .awaitSuspending()
            .toRows()
            .map(::mapMenu)

    private suspend fun requireMenu(
        connection: SqlConnection,
        menuId: UUID,
    ): Menu =
        connection.preparedQuery("$MENU_SELECT_SQL WHERE id = $1")
            .execute(Tuple.of(menuId))
            .awaitSuspending()
            .first()
            .let(::mapMenu)
}

