package io.github.lishangbu.avalon.identity.access.domain.iam

/**
 * 创建或更新用户时使用的输入草稿。
 *
 * @property username 用户名。
 * @property phone 手机号，可为空。
 * @property email 邮箱，可为空。
 * @property avatar 头像地址，可为空。
 * @property enabled 用户是否启用。
 * @property passwordHash 已散列的密码文本，可为空。
 * @property roleIds 需要绑定到用户上的角色标识集合。
 */
data class UserAccountDraft(
    val username: String,
    val phone: String?,
    val email: String?,
    val avatar: String?,
    val enabled: Boolean,
    val passwordHash: String?,
    val roleIds: Set<RoleId>,
)