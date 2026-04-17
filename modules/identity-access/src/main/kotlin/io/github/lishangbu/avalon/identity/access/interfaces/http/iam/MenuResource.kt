package io.github.lishangbu.avalon.identity.access.interfaces.http.iam

import io.github.lishangbu.avalon.identity.access.application.iam.IdentityAccessService
import io.github.lishangbu.avalon.identity.access.application.iam.query.MenuPageQuery
import io.github.lishangbu.avalon.shared.infra.http.pagination.PageResponse
import io.github.lishangbu.avalon.shared.infra.http.pagination.toResponse
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.util.UUID

/**
 * 菜单管理 HTTP 入口。
 */
@Path("/iam/menus")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class MenuResource(
    private val service: IdentityAccessService,
) {
    /**
     * 按固定排序分页列出平铺菜单集合。
     *
     * @param parameters 分页查询参数。
     * @return 菜单分页响应。
     */
    @GET
    suspend fun page(
        @BeanParam
        @Valid
        parameters: IdentityAccessPageParameters,
    ): PageResponse<MenuResponse> =
        service.pageMenus(
            MenuPageQuery(
                pageRequest = parameters.toPageRequest(),
            ),
        ).toResponse { it.toResponse() }

    /**
     * 列出树形菜单。
     *
     * @return 按父子关系装配完成的菜单树。
     */
    @GET
    @Path("/tree")
    suspend fun listTree(): List<MenuTreeNodeResponse> = service.listMenuTree().map { it.toResponse() }

    /**
     * 查询单个菜单。
     *
     * @param id 菜单主键值。
     * @return 命中的菜单响应。
     */
    @GET
    @Path("/{id}")
    suspend fun getById(
        @PathParam("id") id: UUID,
    ): MenuResponse = service.getMenu(id).toResponse()

    /**
     * 创建菜单。
     *
     * @param request 菜单写入请求体。
     * @return 已创建的菜单响应。
     */
    @POST
    suspend fun create(
        @Valid request: UpsertMenuRequest,
    ): MenuResponse = service.createMenu(request.toDraft()).toResponse()

    /**
     * 更新菜单。
     *
     * @param id 菜单主键值。
     * @param request 菜单更新请求体。
     * @return 更新后的菜单响应。
     */
    @PUT
    @Path("/{id}")
    suspend fun update(
        @PathParam("id") id: UUID,
        @Valid request: UpsertMenuRequest,
    ): MenuResponse = service.updateMenu(id, request.toDraft()).toResponse()

    /**
     * 删除菜单。
     *
     * @param id 菜单主键值。
     * @return `204 No Content`。
     */
    @DELETE
    @Path("/{id}")
    suspend fun delete(
        @PathParam("id") id: UUID,
    ): Response {
        service.deleteMenu(id)
        return Response.noContent().build()
    }
}

