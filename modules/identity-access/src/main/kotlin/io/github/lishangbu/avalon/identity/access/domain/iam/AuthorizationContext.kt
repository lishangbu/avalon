package io.github.lishangbu.avalon.identity.access.domain.iam

/**
 * 授权快照构建前的原始装配上下文。
 *
 * 该模型保留平铺菜单集合，供应用层在读取侧装配菜单树。
 *
 * @property user 当前用户主体。
 * @property roles 当前用户角色列表。
 * @property permissions 当前用户权限列表。
 * @property menus 当前用户可访问的平铺菜单列表。
 */
data class AuthorizationContext(
    val user: UserAccount,
    val roles: List<Role>,
    val permissions: List<Permission>,
    val menus: List<Menu>,
)