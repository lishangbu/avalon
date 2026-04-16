package io.github.lishangbu.avalon.identity.access.domain.iam

/**
 * 面向外部读取的授权快照。
 *
 * 这里在保留完整角色、权限和菜单树的同时，额外派生出常用的角色编码
 * 与权限编码集合，减少接口层和调用方重复转换。
 *
 * @property user 当前用户主体。
 * @property roles 当前用户角色列表。
 * @property permissions 当前用户权限列表。
 * @property menuTree 已按父子关系装配好的菜单树。
 */
data class AuthorizationSnapshot(
    val user: UserAccount,
    val roles: List<Role>,
    val permissions: List<Permission>,
    val menuTree: List<MenuTreeNode>,
) {
    val roleCodes: List<String> = roles.map(Role::code)
    val permissionCodes: List<String> = permissions.map(Permission::code)
}