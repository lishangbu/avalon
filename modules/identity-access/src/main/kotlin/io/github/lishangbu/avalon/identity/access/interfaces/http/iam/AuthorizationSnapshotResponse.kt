package io.github.lishangbu.avalon.identity.access.interfaces.http.iam

import io.github.lishangbu.avalon.identity.access.domain.iam.AuthorizationSnapshot
import io.github.lishangbu.avalon.identity.access.domain.iam.MenuTreeNode
import io.github.lishangbu.avalon.identity.access.domain.iam.Permission
import io.github.lishangbu.avalon.identity.access.domain.iam.Role

/**
 * 授权快照响应。
 *
 * @property user 当前用户主体。
 * @property roles 当前用户拥有的角色列表。
 * @property permissions 当前用户拥有的权限列表。
 * @property roleCodes 当前用户角色编码集合。
 * @property permissionCodes 当前用户权限编码集合。
 * @property menuTree 当前用户可见的菜单树。
 */
data class AuthorizationSnapshotResponse(
    val user: UserResponse,
    val roles: List<RoleResponse>,
    val permissions: List<PermissionResponse>,
    val roleCodes: List<String>,
    val permissionCodes: List<String>,
    val menuTree: List<MenuTreeNodeResponse>,
)

/**
 * 将授权快照转换为接口响应。
 *
 * @return 可直接返回给前端的授权初始化视图。
 */
fun AuthorizationSnapshot.toResponse(): AuthorizationSnapshotResponse =
    AuthorizationSnapshotResponse(
        user = user.toResponse(),
        roles = roles.map(Role::toResponse),
        permissions = permissions.map(Permission::toResponse),
        roleCodes = roleCodes,
        permissionCodes = permissionCodes,
        menuTree = menuTree.map(MenuTreeNode::toResponse),
    )