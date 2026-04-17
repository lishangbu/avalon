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
 * 权限管理 HTTP 入口。
 */
@Path("/api/iam/permissions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class PermissionResource(
    private val service: IdentityAccessService,
) {
    /**
     * 按固定排序分页列出权限。
     *
     * @param parameters 分页查询参数。
     * @return 权限分页响应。
     */
    @GET
    suspend fun page(
        @BeanParam
        @Valid
        pageParameters: IdentityAccessPageParameters,
        @BeanParam
        parameters: PermissionQueryParameters,
    ): PageResponse<PermissionResponse> =
        service.pagePermissions(parameters.toPageQuery(pageParameters.toPageRequest())).toResponse { it.toResponse() }

    /**
     * 查找可供管理端选择器消费的权限集合。
     *
     * 这里保留无分页读取，是为了支撑角色授权等轻量 lookup 场景，
     * 并与默认的分页列表接口区分为不同的对外语义。
     *
     * @param parameters 权限查询参数。
     * @return 命中的权限列表响应。
     */
    @GET
    @Path("/lookup")
    suspend fun lookup(
        @BeanParam
        parameters: PermissionQueryParameters,
    ): List<PermissionResponse> = service.listPermissions(parameters.toListQuery()).map { it.toResponse() }

    /**
     * 查询单个权限。
     *
     * @param id 权限主键值。
     * @return 命中的权限响应。
     */
    @GET
    @Path("/{id}")
    suspend fun getById(
        @PathParam("id") id: UUID,
    ): PermissionResponse = service.getPermission(id).toResponse()

    /**
     * 创建权限。
     *
     * @param request 权限写入请求体。
     * @return 已创建的权限响应。
     */
    @POST
    suspend fun create(
        @Valid request: UpsertPermissionRequest,
    ): PermissionResponse = service.createPermission(request.toDraft()).toResponse()

    /**
     * 更新权限。
     *
     * @param id 权限主键值。
     * @param request 权限更新请求体。
     * @return 更新后的权限响应。
     */
    @PUT
    @Path("/{id}")
    suspend fun update(
        @PathParam("id") id: UUID,
        @Valid request: UpsertPermissionRequest,
    ): PermissionResponse = service.updatePermission(id, request.toDraft()).toResponse()

    /**
     * 删除权限。
     *
     * @param id 权限主键值。
     * @return `204 No Content`。
     */
    @DELETE
    @Path("/{id}")
    suspend fun delete(
        @PathParam("id") id: UUID,
    ): Response {
        service.deletePermission(id)
        return Response.noContent().build()
    }
}

