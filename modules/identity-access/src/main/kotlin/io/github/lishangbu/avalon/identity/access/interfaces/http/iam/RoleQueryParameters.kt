package io.github.lishangbu.avalon.identity.access.interfaces.http.iam

import io.github.lishangbu.avalon.identity.access.application.iam.query.RoleListQuery
import io.github.lishangbu.avalon.identity.access.application.iam.query.RolePageQuery
import io.github.lishangbu.avalon.shared.application.query.PageRequest
import jakarta.ws.rs.QueryParam
import java.util.UUID

/**
 * 角色查询参数。
 */
class RoleQueryParameters {
    @field:QueryParam("id")
    var id: UUID? = null

    @field:QueryParam("code")
    var code: String? = null

    @field:QueryParam("name")
    var name: String? = null

    @field:QueryParam("enabled")
    var enabled: Boolean? = null

    fun toPageQuery(pageRequest: PageRequest): RolePageQuery =
        RolePageQuery(
            pageRequest = pageRequest,
            id = id,
            code = code.normalizeQueryText(),
            name = name.normalizeQueryText(),
            enabled = enabled,
        )

    fun toListQuery(): RoleListQuery =
        RoleListQuery(
            id = id,
            code = code.normalizeQueryText(),
            name = name.normalizeQueryText(),
            enabled = enabled,
        )
}

