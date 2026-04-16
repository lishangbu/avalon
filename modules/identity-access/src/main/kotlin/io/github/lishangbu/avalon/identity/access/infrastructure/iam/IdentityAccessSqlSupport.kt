package io.github.lishangbu.avalon.identity.access.infrastructure.iam

import io.github.lishangbu.avalon.identity.access.domain.iam.*
import io.github.lishangbu.avalon.shared.infra.sql.getNullableUUID
import io.vertx.mutiny.sqlclient.Row
import io.vertx.mutiny.sqlclient.Tuple
import java.util.UUID

/**
 * IdentityAccess 的 SQL 支撑函数只服务本上下文。
 *
 * shared-infra 只提供事务和 Vert.x 适配壳；这里保留的仍是 IAM 自己的
 * SQL 常量、领域映射和数据库错误翻译策略，避免把上下文语义抬到共享层。
 */
internal fun mapIdentityAccessDatabaseError(exception: Throwable): Throwable {
    val message = exception.message ?: "IdentityAccess data access failure"
    return when {
        message.contains("duplicate key", ignoreCase = true) ->
            IdentityAccessConflict("The submitted identity access data conflicts with an existing record.")

        message.contains("foreign key", ignoreCase = true) ->
            IdentityAccessConflict("Referenced IAM data does not exist, or the record is still referenced by other IAM data.")

        message.contains("check constraint", ignoreCase = true) ->
            IdentityAccessConflict("The submitted IAM data violates a database validation rule.")

        else -> exception
    }
}

internal fun mapUser(
    row: Row,
    roleIds: Set<RoleId>,
): UserAccount =
    UserAccount(
        id = UserId(row.getUUID("id")),
        username = row.getString("username"),
        phone = row.getString("phone"),
        email = row.getString("email"),
        avatar = row.getString("avatar"),
        enabled = row.getBoolean("enabled"),
        passwordHash = row.getString("password_hash"),
        roleIds = roleIds,
        version = row.getLong("version"),
    )

internal fun mapRole(
    row: Row,
    menuIds: Set<MenuId>,
    permissionIds: Set<PermissionId>,
): Role =
    Role(
        id = RoleId(row.getUUID("id")),
        code = row.getString("code"),
        name = row.getString("name"),
        enabled = row.getBoolean("enabled"),
        menuIds = menuIds,
        permissionIds = permissionIds,
        version = row.getLong("version"),
    )

internal fun mapPermission(row: Row): Permission =
    Permission(
        id = PermissionId(row.getUUID("id")),
        menuId = MenuId(row.getUUID("menu_id")),
        code = row.getString("code"),
        name = row.getString("name"),
        enabled = row.getBoolean("enabled"),
        sortingOrder = row.getInteger("sorting_order"),
        version = row.getLong("version"),
    )

internal fun mapMenu(row: Row): Menu =
    Menu(
        id = MenuId(row.getUUID("id")),
        parentId = row.getNullableUUID("parent_id")?.let(::MenuId),
        disabled = row.getBoolean("disabled"),
        extra = row.getString("extra"),
        icon = row.getString("icon"),
        key = row.getString("menu_key"),
        title = row.getString("title"),
        visible = row.getBoolean("visible"),
        path = row.getString("path"),
        routeName = row.getString("route_name"),
        redirect = row.getString("redirect"),
        component = row.getString("component"),
        sortingOrder = row.getInteger("sorting_order"),
        pinned = row.getBoolean("pinned"),
        showTab = row.getBoolean("show_tab"),
        enableMultiTab = row.getBoolean("enable_multi_tab"),
        type = MenuType.fromStorage(row.getString("menu_type")),
        hidden = row.getBoolean("hidden"),
        hideChildrenInMenu = row.getBoolean("hide_children_in_menu"),
        flatMenu = row.getBoolean("flat_menu"),
        activeMenu = row.getString("active_menu"),
        external = row.getBoolean("external"),
        target = row.getString("target"),
        version = row.getLong("version"),
    )

internal fun menuTuple(draft: MenuDraft): Tuple =
    Tuple.tuple()
        .addValue(draft.parentId?.value)
        .addBoolean(draft.disabled)
        .addValue(draft.extra)
        .addValue(draft.icon)
        .addString(draft.key)
        .addString(draft.title)
        .addBoolean(draft.visible)
        .addValue(draft.path)
        .addValue(draft.routeName)
        .addValue(draft.redirect)
        .addValue(draft.component)
        .addInteger(draft.sortingOrder)
        .addBoolean(draft.pinned)
        .addBoolean(draft.showTab)
        .addBoolean(draft.enableMultiTab)
        .addString(draft.type.storageValue())
        .addBoolean(draft.hidden)
        .addBoolean(draft.hideChildrenInMenu)
        .addBoolean(draft.flatMenu)
        .addValue(draft.activeMenu)
        .addBoolean(draft.external)
        .addValue(draft.target)

internal fun normalizeUsername(value: String): String = value.trim().lowercase()

internal fun normalizeEmail(value: String?): String? = value?.trim()?.takeIf { it.isNotEmpty() }?.lowercase()

internal fun normalizePhone(value: String?): String? {
    val trimmed = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val digits = trimmed.filter { it.isDigit() }
    return when {
        trimmed.startsWith("+") && digits.isNotEmpty() -> "+$digits"
        digits.isNotEmpty() -> digits
        else -> null
    }
}

internal fun toContainsPattern(value: String?): String? =
    value?.trim()?.takeIf { it.isNotEmpty() }?.let { "%${it.lowercase()}%" }

internal fun postgresParameterList(
    firstParameterIndex: Int,
    parameterCount: Int,
): String {
    require(firstParameterIndex >= 1) { "firstParameterIndex must be greater than or equal to 1" }
    require(parameterCount >= 1) { "parameterCount must be greater than or equal to 1" }

    return (0 until parameterCount).joinToString(", ") { offset -> "\$${firstParameterIndex + offset}" }
}

internal fun tupleOfUuids(values: Iterable<UUID>): Tuple =
    values.fold(Tuple.tuple()) { tuple, value -> tuple.addValue(value) }

internal const val USER_SELECT_SQL =
    """
    SELECT
        id,
        username,
        phone,
        email,
        avatar,
        enabled,
        password_hash,
        version
    FROM iam.user_account
    """

internal const val USER_DEFAULT_ORDER_BY_SQL = "ORDER BY id"

internal const val USER_FILTER_SQL =
    """
    WHERE ($1::uuid IS NULL OR id = $1::uuid)
      AND ($2::text IS NULL OR username_normalized LIKE $2::text)
      AND ($3::text IS NULL OR phone_normalized LIKE $3::text)
      AND ($4::text IS NULL OR email_normalized LIKE $4::text)
      AND ($5::boolean IS NULL OR enabled = $5::boolean)
    """

internal const val USER_COUNT_SQL =
    """
    SELECT COUNT(*) AS total_items
    FROM iam.user_account
    $USER_FILTER_SQL
    """

internal const val USER_PAGE_SQL =
    """
    $USER_SELECT_SQL
    $USER_FILTER_SQL
    $USER_DEFAULT_ORDER_BY_SQL
    LIMIT $6 OFFSET $7
    """

internal const val ROLE_SELECT_SQL =
    """
    SELECT
        id,
        code,
        name,
        enabled,
        version
    FROM iam.role
    """

internal const val ROLE_DEFAULT_ORDER_BY_SQL = "ORDER BY code, id"

internal const val ROLE_FILTER_SQL =
    """
    WHERE ($1::uuid IS NULL OR id = $1::uuid)
      AND ($2::text IS NULL OR LOWER(code) LIKE $2::text)
      AND ($3::text IS NULL OR LOWER(name) LIKE $3::text)
      AND ($4::boolean IS NULL OR enabled = $4::boolean)
    """

internal const val ROLE_COUNT_SQL =
    """
    SELECT COUNT(*) AS total_items
    FROM iam.role
    $ROLE_FILTER_SQL
    """

internal const val ROLE_PAGE_SQL =
    """
    $ROLE_SELECT_SQL
    $ROLE_FILTER_SQL
    $ROLE_DEFAULT_ORDER_BY_SQL
    LIMIT $5 OFFSET $6
    """

internal const val ROLE_LIST_SQL =
    """
    $ROLE_SELECT_SQL
    $ROLE_FILTER_SQL
    $ROLE_DEFAULT_ORDER_BY_SQL
    """

internal const val PERMISSION_SELECT_SQL =
    """
    SELECT
        id,
        menu_id,
        code,
        name,
        enabled,
        sorting_order,
        version
    FROM iam.permission
    """

internal const val PERMISSION_DEFAULT_ORDER_BY_SQL = "ORDER BY sorting_order, id"

internal const val PERMISSION_FILTER_SQL =
    """
    WHERE ($1::uuid IS NULL OR id = $1::uuid)
      AND ($2::uuid IS NULL OR menu_id = $2::uuid)
      AND ($3::text IS NULL OR LOWER(code) LIKE $3::text)
      AND ($4::text IS NULL OR LOWER(name) LIKE $4::text)
      AND ($5::boolean IS NULL OR enabled = $5::boolean)
    """

internal const val PERMISSION_COUNT_SQL =
    """
    SELECT COUNT(*) AS total_items
    FROM iam.permission
    $PERMISSION_FILTER_SQL
    """

internal const val PERMISSION_PAGE_SQL =
    """
    $PERMISSION_SELECT_SQL
    $PERMISSION_FILTER_SQL
    $PERMISSION_DEFAULT_ORDER_BY_SQL
    LIMIT $6 OFFSET $7
    """

internal const val PERMISSION_LIST_SQL =
    """
    $PERMISSION_SELECT_SQL
    $PERMISSION_FILTER_SQL
    $PERMISSION_DEFAULT_ORDER_BY_SQL
    """

internal const val MENU_SELECT_SQL =
    """
    SELECT
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
    FROM iam.menu
    """

internal const val MENU_DEFAULT_ORDER_BY_SQL = "ORDER BY sorting_order, id"

internal const val MENU_COUNT_SQL = "SELECT COUNT(*) AS total_items FROM iam.menu"

