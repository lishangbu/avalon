package io.github.lishangbu.avalon.authorization.model

import io.github.lishangbu.avalon.authorization.entity.Role
import io.github.lishangbu.avalon.authorization.entity.User

/**
 * 用户(包含角色信息)
 *
 * @author lishangbu
 * @since 2025/9/19
 */
data class UserWithRoles(
    val id: Long?,
    val username: String?,
    val avatar: String?,
    val roles: Set<PlainRole>,
) {
    constructor(
        user: User,
    ) : this(
        user.id,
        user.username,
        user.avatar,
        user.roles.map(::PlainRole).toSet(),
    )

    data class PlainRole(
        /** 主键 */
        var id: Long? = null,
        /** 角色代码 */
        var code: String? = null,
        /** 角色名称 */
        var name: String? = null,
        /** 角色是否启用 */
        var enabled: Boolean? = null,
    ) {
        constructor(role: Role) : this(role.id, role.code, role.name, role.enabled)
    }
}
