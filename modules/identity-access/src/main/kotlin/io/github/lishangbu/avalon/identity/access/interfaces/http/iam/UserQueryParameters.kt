package io.github.lishangbu.avalon.identity.access.interfaces.http.iam

import io.github.lishangbu.avalon.identity.access.application.iam.query.UserPageQuery
import io.github.lishangbu.avalon.shared.application.query.PageRequest
import jakarta.ws.rs.QueryParam
import java.util.UUID

/**
 * 用户查询参数。
 */
class UserQueryParameters {
    @field:QueryParam("id")
    var id: UUID? = null

    @field:QueryParam("username")
    var username: String? = null

    @field:QueryParam("phone")
    var phone: String? = null

    @field:QueryParam("email")
    var email: String? = null

    @field:QueryParam("enabled")
    var enabled: Boolean? = null

    fun toPageQuery(pageRequest: PageRequest): UserPageQuery =
        UserPageQuery(
            pageRequest = pageRequest,
            id = id,
            username = username.normalizeQueryText(),
            phone = phone.normalizeQueryText(),
            email = email.normalizeQueryText(),
            enabled = enabled,
        )
}

