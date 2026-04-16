package io.github.lishangbu.avalon.catalog.interfaces.http.move

import io.github.lishangbu.avalon.catalog.application.CatalogService
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.util.UUID

/**
 * 招式学习方法维护 HTTP 入口。
 */
@Path("/api/catalog/move-learn-methods")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class MoveLearnMethodResource(
    private val service: CatalogService,
) {
    /**
     * 列出全部招式学习方法。
     *
     * @return 招式学习方法响应列表。
     */
    @GET
    suspend fun list(): List<MoveLearnMethodResponse> = service.listMoveLearnMethods().map { it.toResponse() }

    /**
     * 查询单个招式学习方法。
     *
     * @param id 招式学习方法主键值。
     * @return 命中的招式学习方法响应。
     */
    @GET
    @Path("/{id}")
    suspend fun getById(
        @PathParam("id") id: UUID,
    ): MoveLearnMethodResponse = service.getMoveLearnMethod(id).toResponse()

    /**
     * 创建招式学习方法。
     *
     * @param request 招式学习方法写入请求体。
     * @return 已创建的招式学习方法响应。
     */
    @POST
    suspend fun create(
        @Valid request: UpsertMoveLearnMethodRequest,
    ): MoveLearnMethodResponse = service.createMoveLearnMethod(request.toDraft()).toResponse()

    /**
     * 更新招式学习方法。
     *
     * @param id 招式学习方法主键值。
     * @param request 招式学习方法更新请求体。
     * @return 更新后的招式学习方法响应。
     */
    @PUT
    @Path("/{id}")
    suspend fun update(
        @PathParam("id") id: UUID,
        @Valid request: UpsertMoveLearnMethodRequest,
    ): MoveLearnMethodResponse = service.updateMoveLearnMethod(id, request.toDraft()).toResponse()

    /**
     * 删除招式学习方法。
     *
     * @param id 招式学习方法主键值。
     * @return `204 No Content`。
     */
    @DELETE
    @Path("/{id}")
    suspend fun delete(
        @PathParam("id") id: UUID,
    ): Response {
        service.deleteMoveLearnMethod(id)
        return Response.noContent().build()
    }
}

