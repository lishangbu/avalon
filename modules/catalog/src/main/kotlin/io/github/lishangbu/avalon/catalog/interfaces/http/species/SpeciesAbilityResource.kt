package io.github.lishangbu.avalon.catalog.interfaces.http.species

import io.github.lishangbu.avalon.catalog.application.CatalogService
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.util.UUID

/**
 * 物种特性关联维护 HTTP 入口。
 */
@Path("/api/catalog/species-abilities")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class SpeciesAbilityResource(
    private val service: CatalogService,
) {
    /**
     * 列出全部物种特性关联。
     *
     * @return 物种特性关联响应列表。
     */
    @GET
    suspend fun list(): List<SpeciesAbilityResponse> = service.listSpeciesAbilities().map { it.toResponse() }

    /**
     * 查询单个物种特性关联。
     *
     * @param id 物种特性关联主键值。
     * @return 命中的物种特性关联响应。
     */
    @GET
    @Path("/{id}")
    suspend fun getById(
        @PathParam("id") id: UUID,
    ): SpeciesAbilityResponse = service.getSpeciesAbility(id).toResponse()

    /**
     * 创建物种特性关联。
     *
     * @param request 物种特性关联写入请求体。
     * @return 已创建的物种特性关联响应。
     */
    @POST
    suspend fun create(
        @Valid request: UpsertSpeciesAbilityRequest,
    ): SpeciesAbilityResponse = service.createSpeciesAbility(request.toDraft()).toResponse()

    /**
     * 更新物种特性关联。
     *
     * @param id 物种特性关联主键值。
     * @param request 物种特性关联更新请求体。
     * @return 更新后的物种特性关联响应。
     */
    @PUT
    @Path("/{id}")
    suspend fun update(
        @PathParam("id") id: UUID,
        @Valid request: UpsertSpeciesAbilityRequest,
    ): SpeciesAbilityResponse = service.updateSpeciesAbility(id, request.toDraft()).toResponse()

    /**
     * 删除物种特性关联。
     *
     * @param id 物种特性关联主键值。
     * @return `204 No Content`。
     */
    @DELETE
    @Path("/{id}")
    suspend fun delete(
        @PathParam("id") id: UUID,
    ): Response {
        service.deleteSpeciesAbility(id)
        return Response.noContent().build()
    }
}

