package io.github.lishangbu.avalon.identity.access.interfaces.http.iam

import io.github.lishangbu.avalon.identity.access.application.iam.query.PermissionListQuery
import io.github.lishangbu.avalon.identity.access.application.iam.query.PermissionPageQuery
import io.github.lishangbu.avalon.shared.application.query.PageRequest
import jakarta.ws.rs.QueryParam
import java.util.UUID

/**
 * 权限查询参数。
 */
class PermissionQueryParameters {
    @field:QueryParam("id")
    var id: UUID? = null

    @field:QueryParam("menuId")
    var menuId: UUID? = null

    @field:QueryParam("code")
    var code: String? = null

    @field:QueryParam("name")
    var name: String? = null

    @field:QueryParam("enabled")
    var enabled: Boolean? = null

    fun toPageQuery(pageRequest: PageRequest): PermissionPageQuery =
        PermissionPageQuery(
            pageRequest = pageRequest,
            id = id,
            menuId = menuId,
            code = code.normalizeQueryText(),
            name = name.normalizeQueryText(),
            enabled = enabled,
        )

    fun toListQuery(): PermissionListQuery =
        PermissionListQuery(
            id = id,
            menuId = menuId,
            code = code.normalizeQueryText(),
            name = name.normalizeQueryText(),
            enabled = enabled,
        )
}

