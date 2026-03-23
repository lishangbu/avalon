package io.github.lishangbu.avalon.authorization.model

import io.github.lishangbu.avalon.authorization.entity.Role
import io.github.lishangbu.avalon.authorization.entity.User

/**
 * 用户及角色信息
 *
 * @author lishangbu
 * @since 2025/9/19
 */
data class UserWithRoles(
    /** ID */
    val id: Long?,
    /** 用户名 */
    val username: String?,
    /** 头像 */
    val avatar: String?,
    /** 角色列表 */
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
        /** ID */
        var id: Long? = null,
        /** 状态码 */
        var code: String? = null,
        /** 名称 */
        var name: String? = null,
        /** 启用状态 */
        var enabled: Boolean? = null,
    ) {
        constructor(role: Role) : this(role.id, role.code, role.name, role.enabled)
    }
}
