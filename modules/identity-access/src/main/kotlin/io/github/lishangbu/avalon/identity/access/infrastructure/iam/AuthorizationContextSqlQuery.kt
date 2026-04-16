package io.github.lishangbu.avalon.identity.access.infrastructure.iam

import io.github.lishangbu.avalon.identity.access.domain.iam.AuthorizationContext
import io.github.lishangbu.avalon.identity.access.domain.iam.UserId

/**
 * 授权快照查询负责把用户、角色、权限和菜单一次性装配成读取侧上下文。
 *
 * 这里保留在基础设施层，是因为它本质上是多表 SQL 读取协调，而不是领域行为本身。
 */
internal class AuthorizationContextSqlQuery(
    private val userGateway: UserSqlGateway,
    private val roleGateway: RoleSqlGateway,
    private val permissionGateway: PermissionSqlGateway,
    private val menuGateway: MenuSqlGateway,
) {
    suspend fun findAuthorizationContext(userId: UserId): AuthorizationContext? {
        val user = userGateway.findUser(userId) ?: return null
        val roles = roleGateway.loadRolesByUser(userId.value)
        val permissions = permissionGateway.loadPermissionsByUser(userId.value)
        val menus = menuGateway.loadAuthorizedMenus(userId.value)
        val authorizedMenuIds = menus.map { it.id.value }.toSet()
        val authorizedPermissionIds = permissions.map { it.id.value }.toSet()

        return AuthorizationContext(
            user = user,
            roles = roles.map { role ->
                role.copy(
                    menuIds = role.menuIds.filter { it.value in authorizedMenuIds }.toSet(),
                    permissionIds = role.permissionIds.filter { it.value in authorizedPermissionIds }.toSet(),
                )
            },
            permissions = permissions,
            menus = menus,
        )
    }
}