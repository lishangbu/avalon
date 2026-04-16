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
 * 用户管理 HTTP 入口。
 */
@Path("/api/iam/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class UserResource(
    private val service: IdentityAccessService,
) {
    /**
     * 按固定排序分页列出用户。
     *
     * @param parameters 分页查询参数。
     * @return 用户分页响应。
     */
    @GET
    suspend fun page(
        @BeanParam
        @Valid
        pageParameters: IdentityAccessPageParameters,
        @BeanParam
        parameters: UserQueryParameters,
    ): PageResponse<UserResponse> =
        service.pageUsers(parameters.toPageQuery(pageParameters.toPageRequest())).toResponse { it.toResponse() }

    /**
     * 查询单个用户。
     *
     * @param id 用户主键值。
     * @return 命中的用户响应。
     */
    @GET
    @Path("/{id}")
    suspend fun getById(
        @PathParam("id") id: UUID,
    ): UserResponse = service.getUser(id).toResponse()

    /**
     * 创建用户。
     *
     * @param request 用户写入请求体。
     * @return 已创建的用户响应。
     */
    @POST
    suspend fun create(
        @Valid request: UpsertUserRequest,
    ): UserResponse = service.createUser(request.toDraft()).toResponse()

    /**
     * 更新用户。
     *
     * @param id 用户主键值。
     * @param request 用户更新请求体。
     * @return 更新后的用户响应。
     */
    @PUT
    @Path("/{id}")
    suspend fun update(
        @PathParam("id") id: UUID,
        @Valid request: UpsertUserRequest,
    ): UserResponse = service.updateUser(id, request.toDraft()).toResponse()

    /**
     * 删除用户。
     *
     * @param id 用户主键值。
     * @return `204 No Content`。
     */
    @DELETE
    @Path("/{id}")
    suspend fun delete(
        @PathParam("id") id: UUID,
    ): Response {
        service.deleteUser(id)
        return Response.noContent().build()
    }
}

