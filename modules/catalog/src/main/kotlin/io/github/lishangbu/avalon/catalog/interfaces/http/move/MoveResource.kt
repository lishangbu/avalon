package io.github.lishangbu.avalon.catalog.interfaces.http.move

import io.github.lishangbu.avalon.catalog.application.CatalogService
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.util.UUID

/**
 * 技能定义维护 HTTP 入口。
 */
@Path("/catalog/moves")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class MoveResource(
    private val service: CatalogService,
) {
    /**
     * 列出全部技能定义。
     *
     * @return 技能定义响应列表。
     */
    @GET
    suspend fun list(): List<MoveResponse> = service.listMoves().map { it.toResponse() }

    /**
     * 查询单个技能定义。
     *
     * @param id 技能定义主键值。
     * @return 命中的技能定义响应。
     */
    @GET
    @Path("/{id}")
    suspend fun getById(
        @PathParam("id") id: UUID,
    ): MoveResponse = service.getMove(id).toResponse()

    /**
     * 创建技能定义。
     *
     * @param request 技能写入请求体。
     * @return 已创建的技能定义响应。
     */
    @POST
    suspend fun create(
        @Valid request: UpsertMoveRequest,
    ): MoveResponse = service.createMove(request.toDraft()).toResponse()

    /**
     * 更新技能定义。
     *
     * @param id 技能定义主键值。
     * @param request 技能更新请求体。
     * @return 更新后的技能定义响应。
     */
    @PUT
    @Path("/{id}")
    suspend fun update(
        @PathParam("id") id: UUID,
        @Valid request: UpsertMoveRequest,
    ): MoveResponse = service.updateMove(id, request.toDraft()).toResponse()

    /**
     * 删除技能定义。
     *
     * @param id 技能定义主键值。
     * @return `204 No Content`。
     */
    @DELETE
    @Path("/{id}")
    suspend fun delete(
        @PathParam("id") id: UUID,
    ): Response {
        service.deleteMove(id)
        return Response.noContent().build()
    }
}

