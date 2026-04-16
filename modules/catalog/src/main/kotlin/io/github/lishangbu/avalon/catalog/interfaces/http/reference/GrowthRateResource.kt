package io.github.lishangbu.avalon.catalog.interfaces.http.reference

import io.github.lishangbu.avalon.catalog.application.CatalogService
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.util.UUID

/**
 * 成长率定义维护 HTTP 入口。
 */
@Path("/api/catalog/growth-rates")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class GrowthRateResource(
    private val service: CatalogService,
) {
    /**
     * 列出全部成长率定义。
     *
     * @return 成长率定义响应列表。
     */
    @GET
    suspend fun list(): List<GrowthRateResponse> = service.listGrowthRates().map { it.toResponse() }

    /**
     * 查询单个成长率定义。
     *
     * @param id 成长率定义主键值。
     * @return 命中的成长率定义响应。
     */
    @GET
    @Path("/{id}")
    suspend fun getById(
        @PathParam("id") id: UUID,
    ): GrowthRateResponse = service.getGrowthRate(id).toResponse()

    /**
     * 创建成长率定义。
     *
     * @param request 成长率写入请求体。
     * @return 已创建的成长率定义响应。
     */
    @POST
    suspend fun create(
        @Valid request: UpsertGrowthRateRequest,
    ): GrowthRateResponse = service.createGrowthRate(request.toDraft()).toResponse()

    /**
     * 更新成长率定义。
     *
     * @param id 成长率定义主键值。
     * @param request 成长率更新请求体。
     * @return 更新后的成长率定义响应。
     */
    @PUT
    @Path("/{id}")
    suspend fun update(
        @PathParam("id") id: UUID,
        @Valid request: UpsertGrowthRateRequest,
    ): GrowthRateResponse = service.updateGrowthRate(id, request.toDraft()).toResponse()

    /**
     * 删除成长率定义。
     *
     * @param id 成长率定义主键值。
     * @return `204 No Content`。
     */
    @DELETE
    @Path("/{id}")
    suspend fun delete(
        @PathParam("id") id: UUID,
    ): Response {
        service.deleteGrowthRate(id)
        return Response.noContent().build()
    }
}

