package io.github.lishangbu.avalon.catalog.interfaces.http.reference

import io.github.lishangbu.avalon.catalog.application.CatalogService
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.util.UUID

/**
 * 属性克制关系维护 HTTP 入口。
 */
@Path("/api/catalog/type-effectiveness")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class TypeEffectivenessResource(
    private val service: CatalogService,
) {
    /**
     * 列出全部属性克制关系。
     *
     * @return 属性克制关系响应列表。
     */
    @GET
    suspend fun list(): List<TypeEffectivenessResponse> = service.listTypeEffectiveness().map { it.toResponse() }

    /**
     * 查询单个属性克制关系。
     *
     * @param id 属性克制关系主键值。
     * @return 命中的属性克制关系响应。
     */
    @GET
    @Path("/{id}")
    suspend fun getById(
        @PathParam("id") id: UUID,
    ): TypeEffectivenessResponse = service.getTypeEffectiveness(id).toResponse()

    /**
     * 创建属性克制关系。
     *
     * @param request 属性克制关系写入请求体。
     * @return 已创建的属性克制关系响应。
     */
    @POST
    suspend fun create(
        @Valid request: UpsertTypeEffectivenessRequest,
    ): TypeEffectivenessResponse = service.createTypeEffectiveness(request.toDraft()).toResponse()

    /**
     * 更新属性克制关系。
     *
     * @param id 属性克制关系主键值。
     * @param request 属性克制关系更新请求体。
     * @return 更新后的属性克制关系响应。
     */
    @PUT
    @Path("/{id}")
    suspend fun update(
        @PathParam("id") id: UUID,
        @Valid request: UpsertTypeEffectivenessRequest,
    ): TypeEffectivenessResponse = service.updateTypeEffectiveness(id, request.toDraft()).toResponse()

    /**
     * 删除属性克制关系。
     *
     * @param id 属性克制关系主键值。
     * @return `204 No Content`。
     */
    @DELETE
    @Path("/{id}")
    suspend fun delete(
        @PathParam("id") id: UUID,
    ): Response {
        service.deleteTypeEffectiveness(id)
        return Response.noContent().build()
    }
}

