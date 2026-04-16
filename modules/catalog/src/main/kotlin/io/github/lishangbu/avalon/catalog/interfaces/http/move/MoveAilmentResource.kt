package io.github.lishangbu.avalon.catalog.interfaces.http.move

import io.github.lishangbu.avalon.catalog.application.CatalogService
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.util.UUID

/**
 * 招式异常状态定义的 HTTP 入口。
 */
@Path("/api/catalog/move-ailments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class MoveAilmentResource(
    private val service: CatalogService,
) {
    /**
     * 列出全部招式异常状态定义。
     */
    @GET
    suspend fun list(): List<MoveAilmentResponse> = service.listMoveAilments().map { it.toResponse() }

    /**
     * 查询单个招式异常状态定义。
     */
    @GET
    @Path("/{id}")
    suspend fun getById(
        @PathParam("id") id: UUID,
    ): MoveAilmentResponse = service.getMoveAilment(id).toResponse()

    /**
     * 创建招式异常状态定义。
     */
    @POST
    suspend fun create(
        @Valid request: UpsertMoveAilmentRequest,
    ): MoveAilmentResponse = service.createMoveAilment(request.toDraft()).toResponse()

    /**
     * 更新招式异常状态定义。
     */
    @PUT
    @Path("/{id}")
    suspend fun update(
        @PathParam("id") id: UUID,
        @Valid request: UpsertMoveAilmentRequest,
    ): MoveAilmentResponse = service.updateMoveAilment(id, request.toDraft()).toResponse()

    /**
     * 删除招式异常状态定义。
     */
    @DELETE
    @Path("/{id}")
    suspend fun delete(
        @PathParam("id") id: UUID,
    ): Response {
        service.deleteMoveAilment(id)
        return Response.noContent().build()
    }
}

