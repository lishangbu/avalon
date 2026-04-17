package io.github.lishangbu.avalon.catalog.interfaces.http.move

import io.github.lishangbu.avalon.catalog.application.CatalogService
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.util.UUID

/**
 * 招式目标定义的 HTTP 入口。
 */
@Path("/catalog/move-targets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class MoveTargetResource(
    private val service: CatalogService,
) {
    /**
     * 列出全部招式目标定义。
     *
     * @return 招式目标列表响应。
     */
    @GET
    suspend fun list(): List<MoveTargetResponse> = service.listMoveTargets().map { it.toResponse() }

    /**
     * 查询单个招式目标定义。
     *
     * @param id 招式目标主键值。
     * @return 命中的招式目标响应。
     */
    @GET
    @Path("/{id}")
    suspend fun getById(
        @PathParam("id") id: UUID,
    ): MoveTargetResponse = service.getMoveTarget(id).toResponse()

    /**
     * 创建招式目标定义。
     *
     * @param request 招式目标写入请求体。
     * @return 已创建的招式目标响应。
     */
    @POST
    suspend fun create(
        @Valid request: UpsertMoveTargetRequest,
    ): MoveTargetResponse = service.createMoveTarget(request.toDraft()).toResponse()

    /**
     * 更新招式目标定义。
     *
     * @param id 招式目标主键值。
     * @param request 招式目标更新请求体。
     * @return 更新后的招式目标响应。
     */
    @PUT
    @Path("/{id}")
    suspend fun update(
        @PathParam("id") id: UUID,
        @Valid request: UpsertMoveTargetRequest,
    ): MoveTargetResponse = service.updateMoveTarget(id, request.toDraft()).toResponse()

    /**
     * 删除招式目标定义。
     *
     * @param id 招式目标主键值。
     * @return `204 No Content`。
     */
    @DELETE
    @Path("/{id}")
    suspend fun delete(
        @PathParam("id") id: UUID,
    ): Response {
        service.deleteMoveTarget(id)
        return Response.noContent().build()
    }
}

