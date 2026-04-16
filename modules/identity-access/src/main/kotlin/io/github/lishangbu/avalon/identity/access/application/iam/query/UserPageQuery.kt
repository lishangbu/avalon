package io.github.lishangbu.avalon.identity.access.application.iam.query

import io.github.lishangbu.avalon.shared.application.query.PageRequest
import java.util.UUID

/**
 * 用户列表分页查询契约。
 *
 * @property pageRequest 当前查询使用的分页请求。
 * @property id 用户主键过滤条件。
 * @property username 用户名关键字过滤条件。
 * @property phone 手机号关键字过滤条件。
 * @property email 邮箱关键字过滤条件。
 * @property enabled 启用状态过滤条件。
 */
data class UserPageQuery(
    val pageRequest: PageRequest = PageRequest(),
    val id: UUID? = null,
    val username: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val enabled: Boolean? = null,
)
