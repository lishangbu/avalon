package io.github.lishangbu.avalon.catalog.interfaces.http.reference

import io.github.lishangbu.avalon.catalog.application.CatalogService
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.util.UUID

/**
 * 特性定义维护 HTTP 入口。
 */
@Path("/catalog/abilities")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class AbilityResource(
    private val service: CatalogService,
) {
    /**
     * 列出全部特性定义。
     *
     * @return 特性定义响应列表。
     */
    @GET
    suspend fun list(): List<AbilityResponse> = service.listAbilities().map { it.toResponse() }

    /**
     * 查询单个特性定义。
     *
     * @param id 特性定义主键值。
     * @return 命中的特性定义响应。
     */
    @GET
    @Path("/{id}")
    suspend fun getById(
        @PathParam("id") id: UUID,
    ): AbilityResponse = service.getAbility(id).toResponse()

    /**
     * 创建特性定义。
     *
     * @param request 特性写入请求体。
     * @return 已创建的特性定义响应。
     */
    @POST
    suspend fun create(
        @Valid request: UpsertAbilityRequest,
    ): AbilityResponse = service.createAbility(request.toDraft()).toResponse()

    /**
     * 更新特性定义。
     *
     * @param id 特性定义主键值。
     * @param request 特性更新请求体。
     * @return 更新后的特性定义响应。
     */
    @PUT
    @Path("/{id}")
    suspend fun update(
        @PathParam("id") id: UUID,
        @Valid request: UpsertAbilityRequest,
    ): AbilityResponse = service.updateAbility(id, request.toDraft()).toResponse()

    /**
     * 删除特性定义。
     *
     * @param id 特性定义主键值。
     * @return `204 No Content`。
     */
    @DELETE
    @Path("/{id}")
    suspend fun delete(
        @PathParam("id") id: UUID,
    ): Response {
        service.deleteAbility(id)
        return Response.noContent().build()
    }
}

