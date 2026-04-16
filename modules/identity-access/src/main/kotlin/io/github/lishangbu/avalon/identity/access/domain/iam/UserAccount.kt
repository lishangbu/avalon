package io.github.lishangbu.avalon.identity.access.domain.iam


/**
 * 用户聚合的领域视图。
 *
 * @property id 用户标识。
 * @property username 用户名。
 * @property phone 手机号，可为空。
 * @property email 邮箱，可为空。
 * @property avatar 头像地址，可为空。
 * @property enabled 用户是否启用。
 * @property passwordHash 已保存的密码散列，可为空。
 * @property roleIds 当前绑定的角色标识集合。
 * @property version 乐观锁版本号。
 */
data class UserAccount(
    val id: UserId,
    val username: String,
    val phone: String?,
    val email: String?,
    val avatar: String?,
    val enabled: Boolean,
    val passwordHash: String?,
    val roleIds: Set<RoleId>,
    val version: Long,
)