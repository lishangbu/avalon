package io.github.lishangbu.avalon.identity.access.interfaces.http.iam

import io.github.lishangbu.avalon.identity.access.application.iam.IdentityAccessService
import io.github.lishangbu.avalon.shared.infra.http.pagination.PageResponse
import io.github.lishangbu.avalon.shared.infra.http.pagination.toResponse
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.util.UUID

/**
 * 角色管理 HTTP 入口。
 */
@Path("/iam/roles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class RoleResource(
    private val service: IdentityAccessService,
) {
    /**
     * 按固定排序分页列出角色。
     *
     * @param parameters 分页查询参数。
     * @return 角色分页响应。
     */
    @GET
    suspend fun page(
        @BeanParam
        @Valid
        pageParameters: IdentityAccessPageParameters,
        @BeanParam
        parameters: RoleQueryParameters,
    ): PageResponse<RoleResponse> =
        service.pageRoles(parameters.toPageQuery(pageParameters.toPageRequest())).toResponse { it.toResponse() }

    /**
     * 查找可供管理端选择器消费的角色集合。
     *
     * 这里保留无分页读取，是为了支撑角色关联选择等轻量 lookup 场景，
     * 并与默认的分页列表接口区分为不同的对外语义。
     *
     * @param parameters 角色查询参数。
     * @return 命中的角色列表响应。
     */
    @GET
    @Path("/lookup")
    suspend fun lookup(
        @BeanParam
        parameters: RoleQueryParameters,
    ): List<RoleResponse> = service.listRoles(parameters.toListQuery()).map { it.toResponse() }

    /**
     * 查询单个角色。
     *
     * @param id 角色主键值。
     * @return 命中的角色响应。
     */
    @GET
    @Path("/{id}")
    suspend fun getById(
        @PathParam("id") id: UUID,
    ): RoleResponse = service.getRole(id).toResponse()

    /**
     * 创建角色。
     *
     * @param request 角色写入请求体。
     * @return 已创建的角色响应。
     */
    @POST
    suspend fun create(
        @Valid request: UpsertRoleRequest,
    ): RoleResponse = service.createRole(request.toDraft()).toResponse()

    /**
     * 更新角色。
     *
     * @param id 角色主键值。
     * @param request 角色更新请求体。
     * @return 更新后的角色响应。
     */
    @PUT
    @Path("/{id}")
    suspend fun update(
        @PathParam("id") id: UUID,
        @Valid request: UpsertRoleRequest,
    ): RoleResponse = service.updateRole(id, request.toDraft()).toResponse()

    /**
     * 删除角色。
     *
     * @param id 角色主键值。
     * @return `204 No Content`。
     */
    @DELETE
    @Path("/{id}")
    suspend fun delete(
        @PathParam("id") id: UUID,
    ): Response {
        service.deleteRole(id)
        return Response.noContent().build()
    }
}

