package io.github.lishangbu.avalon.identity.access.application.iam.query

import io.github.lishangbu.avalon.shared.application.query.PageRequest
import java.util.UUID

/**
 * 权限列表分页查询契约。
 *
 * @property pageRequest 当前查询使用的分页请求。
 * @property id 权限主键过滤条件。
 * @property menuId 所属菜单主键过滤条件。
 * @property code 权限编码关键字过滤条件。
 * @property name 权限名称关键字过滤条件。
 * @property enabled 启用状态过滤条件。
 */
data class PermissionPageQuery(
    val pageRequest: PageRequest = PageRequest(),
    val id: UUID? = null,
    val menuId: UUID? = null,
    val code: String? = null,
    val name: String? = null,
    val enabled: Boolean? = null,
)
