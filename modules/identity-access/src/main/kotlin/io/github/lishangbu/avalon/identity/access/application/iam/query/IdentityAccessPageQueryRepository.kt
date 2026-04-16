package io.github.lishangbu.avalon.identity.access.application.iam.query

import io.github.lishangbu.avalon.identity.access.domain.iam.Menu
import io.github.lishangbu.avalon.identity.access.domain.iam.Permission
import io.github.lishangbu.avalon.identity.access.domain.iam.Role
import io.github.lishangbu.avalon.identity.access.domain.iam.UserAccount
import io.github.lishangbu.avalon.shared.application.query.Page

/**
 * IAM 管理端读取查询端口。
 *
 * 这里承载用户、角色、权限和平铺菜单列表的读取契约，
 * 避免把 offset 分页策略与列表读取继续塞进写模型仓储接口。
 */
interface IdentityAccessPageQueryRepository {
    /**
     * 按固定排序分页读取用户列表。
     */
    suspend fun pageUsers(query: UserPageQuery): Page<UserAccount>

    /**
     * 按固定排序分页读取角色列表。
     */
    suspend fun pageRoles(query: RolePageQuery): Page<Role>

    /**
     * 按固定排序读取角色列表。
     */
    suspend fun listRoles(query: RoleListQuery): List<Role>

    /**
     * 按固定排序分页读取权限列表。
     */
    suspend fun pagePermissions(query: PermissionPageQuery): Page<Permission>

    /**
     * 按固定排序读取权限列表。
     */
    suspend fun listPermissions(query: PermissionListQuery): List<Permission>

    /**
     * 按固定排序分页读取平铺菜单列表。
     */
    suspend fun pageMenus(query: MenuPageQuery): Page<Menu>
}