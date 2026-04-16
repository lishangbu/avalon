package io.github.lishangbu.avalon.catalog.interfaces.http.species

import io.github.lishangbu.avalon.catalog.application.CatalogService
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.util.UUID

/**
 * 物种进化定义维护 HTTP 入口。
 */
@Path("/api/catalog/species-evolutions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class SpeciesEvolutionResource(
    private val service: CatalogService,
) {
    /**
     * 列出全部物种进化定义。
     *
     * @return 物种进化定义响应列表。
     */
    @GET
    suspend fun list(): List<SpeciesEvolutionResponse> = service.listSpeciesEvolutions().map { it.toResponse() }

    /**
     * 查询单个物种进化定义。
     *
     * @param id 物种进化定义主键值。
     * @return 命中的物种进化定义响应。
     */
    @GET
    @Path("/{id}")
    suspend fun getById(
        @PathParam("id") id: UUID,
    ): SpeciesEvolutionResponse = service.getSpeciesEvolution(id).toResponse()

    /**
     * 创建物种进化定义。
     *
     * @param request 物种进化写入请求体。
     * @return 已创建的物种进化定义响应。
     */
    @POST
    suspend fun create(
        @Valid request: UpsertSpeciesEvolutionRequest,
    ): SpeciesEvolutionResponse = service.createSpeciesEvolution(request.toDraft()).toResponse()

    /**
     * 更新物种进化定义。
     *
     * @param id 物种进化定义主键值。
     * @param request 物种进化更新请求体。
     * @return 更新后的物种进化定义响应。
     */
    @PUT
    @Path("/{id}")
    suspend fun update(
        @PathParam("id") id: UUID,
        @Valid request: UpsertSpeciesEvolutionRequest,
    ): SpeciesEvolutionResponse = service.updateSpeciesEvolution(id, request.toDraft()).toResponse()

    /**
     * 删除物种进化定义。
     *
     * @param id 物种进化定义主键值。
     * @return `204 No Content`。
     */
    @DELETE
    @Path("/{id}")
    suspend fun delete(
        @PathParam("id") id: UUID,
    ): Response {
        service.deleteSpeciesEvolution(id)
        return Response.noContent().build()
    }
}

