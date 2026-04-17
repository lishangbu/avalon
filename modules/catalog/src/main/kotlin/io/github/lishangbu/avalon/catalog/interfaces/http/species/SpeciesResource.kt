package io.github.lishangbu.avalon.catalog.interfaces.http.species

import io.github.lishangbu.avalon.catalog.application.CatalogService
import io.github.lishangbu.avalon.shared.infra.http.pagination.PageResponse
import io.github.lishangbu.avalon.shared.infra.http.pagination.toResponse
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.util.UUID

/**
 * 物种定义维护 HTTP 入口。
 */
@Path("/catalog/species")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class SpeciesResource(
    private val service: CatalogService,
) {
    /**
     * 按固定排序分页列出物种定义。
     *
     * 当前暂不开放客户端自定义排序，统一按 `sortingOrder, id` 返回，
     * 以保证跨页读取时顺序稳定。
     *
     * @param parameters 分页查询参数。
     * @return 包含物种定义与分页元数据的响应。
     */
    @GET
    suspend fun page(
        @BeanParam
        @Valid
        parameters: SpeciesPageParameters,
    ): PageResponse<SpeciesResponse> =
        service.pageSpecies(parameters.toQuery())
            .toResponse { it.toResponse() }

    /**
     * 查询单个物种定义。
     *
     * @param id 物种定义主键值。
     * @return 命中的物种定义响应。
     */
    @GET
    @Path("/{id}")
    suspend fun getById(
        @PathParam("id") id: UUID,
    ): SpeciesResponse = service.getSpecies(id).toResponse()

    /**
     * 创建物种定义。
     *
     * @param request 物种写入请求体。
     * @return 已创建的物种定义响应。
     */
    @POST
    suspend fun create(
        @Valid request: UpsertSpeciesRequest,
    ): SpeciesResponse = service.createSpecies(request.toDraft()).toResponse()

    /**
     * 更新物种定义。
     *
     * @param id 物种定义主键值。
     * @param request 物种更新请求体。
     * @return 更新后的物种定义响应。
     */
    @PUT
    @Path("/{id}")
    suspend fun update(
        @PathParam("id") id: UUID,
        @Valid request: UpsertSpeciesRequest,
    ): SpeciesResponse = service.updateSpecies(id, request.toDraft()).toResponse()

    /**
     * 删除物种定义。
     *
     * @param id 物种定义主键值。
     * @return `204 No Content`。
     */
    @DELETE
    @Path("/{id}")
    suspend fun delete(
        @PathParam("id") id: UUID,
    ): Response {
        service.deleteSpecies(id)
        return Response.noContent().build()
    }
}

