package io.github.lishangbu.avalon.catalog.interfaces.http.species

import io.github.lishangbu.avalon.catalog.application.CatalogService
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.util.UUID

/**
 * 物种招式学习关系维护 HTTP 入口。
 */
@Path("/catalog/species-move-learnsets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class SpeciesMoveLearnsetResource(
    private val service: CatalogService,
) {
    /**
     * 列出全部物种招式学习关系。
     *
     * @return 物种招式学习关系响应列表。
     */
    @GET
    suspend fun list(): List<SpeciesMoveLearnsetResponse> = service.listSpeciesMoveLearnsets().map { it.toResponse() }

    /**
     * 查询单个物种招式学习关系。
     *
     * @param id 关系主键值。
     * @return 命中的物种招式学习关系响应。
     */
    @GET
    @Path("/{id}")
    suspend fun getById(
        @PathParam("id") id: UUID,
    ): SpeciesMoveLearnsetResponse = service.getSpeciesMoveLearnset(id).toResponse()

    /**
     * 创建物种招式学习关系。
     *
     * @param request 物种招式学习关系写入请求体。
     * @return 已创建的物种招式学习关系响应。
     */
    @POST
    suspend fun create(
        @Valid request: UpsertSpeciesMoveLearnsetRequest,
    ): SpeciesMoveLearnsetResponse = service.createSpeciesMoveLearnset(request.toDraft()).toResponse()

    /**
     * 更新物种招式学习关系。
     *
     * @param id 关系主键值。
     * @param request 物种招式学习关系更新请求体。
     * @return 更新后的物种招式学习关系响应。
     */
    @PUT
    @Path("/{id}")
    suspend fun update(
        @PathParam("id") id: UUID,
        @Valid request: UpsertSpeciesMoveLearnsetRequest,
    ): SpeciesMoveLearnsetResponse = service.updateSpeciesMoveLearnset(id, request.toDraft()).toResponse()

    /**
     * 删除物种招式学习关系。
     *
     * @param id 关系主键值。
     * @return `204 No Content`。
     */
    @DELETE
    @Path("/{id}")
    suspend fun delete(
        @PathParam("id") id: UUID,
    ): Response {
        service.deleteSpeciesMoveLearnset(id)
        return Response.noContent().build()
    }
}

