package io.github.lishangbu.avalon.identity.access.interfaces.http.auth

import io.github.lishangbu.avalon.identity.access.domain.authentication.CurrentAuthenticatedUser
import io.github.lishangbu.avalon.identity.access.interfaces.http.iam.*

/**
 * 当前认证用户视图。
 *
 * 该响应把当前会话、用户主体、角色、权限和菜单树快照一起返回，
 * 让前端初始化鉴权状态时不必再额外拼装多个接口结果。
 *
 * @property sessionId 当前访问令牌对应的会话标识。
 * @property user 当前登录用户。
 * @property roles 当前用户具备的角色列表。
 * @property permissions 当前用户具备的权限列表。
 * @property roleCodes 角色编码集合，便于前端快速判断。
 * @property permissionCodes 权限编码集合，便于前端快速判断。
 * @property menuTree 已按授权过滤后的菜单树。
 */
data class CurrentUserResponse(
    val sessionId: String,
    val user: UserResponse,
    val roles: List<RoleResponse>,
    val permissions: List<PermissionResponse>,
    val roleCodes: List<String>,
    val permissionCodes: List<String>,
    val menuTree: List<MenuTreeNodeResponse>,
)

/**
 * 把当前认证用户上下文转换为前端初始化所需的授权视图。
 *
 * @return 包含用户主体、角色、权限和菜单树的响应对象。
 */
fun CurrentAuthenticatedUser.toResponse(): CurrentUserResponse =
    CurrentUserResponse(
        sessionId = sessionId,
        user = snapshot.user.toResponse(),
        roles = snapshot.roles.map { it.toResponse() },
        permissions = snapshot.permissions.map { it.toResponse() },
        roleCodes = snapshot.roleCodes,
        permissionCodes = snapshot.permissionCodes,
        menuTree = snapshot.menuTree.map { it.toResponse() },
    )