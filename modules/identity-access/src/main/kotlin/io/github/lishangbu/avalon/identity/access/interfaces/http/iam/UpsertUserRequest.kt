package io.github.lishangbu.avalon.identity.access.interfaces.http.iam

import io.github.lishangbu.avalon.identity.access.domain.iam.RoleId
import io.github.lishangbu.avalon.identity.access.domain.iam.UserAccountDraft
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

/**
 * 用户创建或更新请求。
 *
 * @property username 用户名，会在映射到领域草稿时做裁剪。
 * @property phone 手机号，可为空。
 * @property email 邮箱，可为空。
 * @property avatar 头像地址，可为空。
 * @property enabled 当前用户是否启用。
 * @property passwordHash 已散列的密码文本，可为空。
 * @property roleIds 需要绑定到用户上的角色主键集合。
 */
data class UpsertUserRequest(
    @field:NotBlank
    @field:Size(max = 64)
    val username: String,
    @field:Size(max = 32)
    val phone: String? = null,
    @field:Email
    @field:Size(max = 255)
    val email: String? = null,
    @field:Size(max = 512)
    val avatar: String? = null,
    val enabled: Boolean = true,
    @field:Size(max = 255)
    val passwordHash: String? = null,
    val roleIds: Set<UUID> = emptySet(),
)

/**
 * 将用户请求转换为领域草稿。
 *
 * @return 已完成空白裁剪和角色主键封装的用户草稿。
 */
fun UpsertUserRequest.toDraft(): UserAccountDraft =
    UserAccountDraft(
        username = username.trim(),
        phone = phone?.trim()?.takeIf { it.isNotEmpty() },
        email = email?.trim()?.takeIf { it.isNotEmpty() },
        avatar = avatar?.trim()?.takeIf { it.isNotEmpty() },
        enabled = enabled,
        passwordHash = passwordHash?.trim()?.takeIf { it.isNotEmpty() },
        roleIds = roleIds.map(::RoleId).toSet(),
    )

