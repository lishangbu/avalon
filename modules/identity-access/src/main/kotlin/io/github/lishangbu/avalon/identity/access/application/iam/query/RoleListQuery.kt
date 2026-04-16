package io.github.lishangbu.avalon.identity.access.application.iam.query

import java.util.UUID

/**
 * 角色列表查询契约。
 *
 * @property id 角色主键过滤条件。
 * @property code 角色编码关键字过滤条件。
 * @property name 角色名称关键字过滤条件。
 * @property enabled 启用状态过滤条件。
 */
data class RoleListQuery(
    val id: UUID? = null,
    val code: String? = null,
    val name: String? = null,
    val enabled: Boolean? = null,
)

