package io.github.lishangbu.avalon.identity.access.interfaces.http.iam

import io.github.lishangbu.avalon.identity.access.domain.iam.UserAccount
import java.util.UUID

/**
 * 用户响应。
 *
 * @property id 用户主键。
 * @property username 用户名。
 * @property phone 手机号，可为空。
 * @property email 邮箱，可为空。
 * @property avatar 头像地址，可为空。
 * @property enabled 用户是否启用。
 * @property roleIds 当前绑定的角色主键列表。
 * @property version 乐观锁版本号。
 */
data class UserResponse(
    val id: UUID,
    val username: String,
    val phone: String?,
    val email: String?,
    val avatar: String?,
    val enabled: Boolean,
    val roleIds: List<UUID>,
    val version: Long,
)

/**
 * 将用户领域对象转换为接口响应。
 *
 * @return 面向管理端的用户视图。
 */
fun UserAccount.toResponse(): UserResponse =
    UserResponse(
        id = id.value,
        username = username,
        phone = phone,
        email = email,
        avatar = avatar,
        enabled = enabled,
        roleIds = roleIds.map { it.value }.sorted(),
        version = version,
    )
