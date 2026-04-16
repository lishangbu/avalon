package io.github.lishangbu.avalon.catalog.interfaces.http.reference

import io.github.lishangbu.avalon.catalog.application.CatalogService
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.util.UUID

/**
 * 性格定义维护 HTTP 入口。
 */
@Path("/api/catalog/natures")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class NatureResource(
    private val service: CatalogService,
) {
    /**
     * 列出全部性格定义。
     *
     * @return 性格定义响应列表。
     */
    @GET
    suspend fun list(): List<NatureResponse> = service.listNatures().map { it.toResponse() }

    /**
     * 查询单个性格定义。
     *
     * @param id 性格定义主键值。
     * @return 命中的性格定义响应。
     */
    @GET
    @Path("/{id}")
    suspend fun getById(
        @PathParam("id") id: UUID,
    ): NatureResponse = service.getNature(id).toResponse()

    /**
     * 创建性格定义。
     *
     * @param request 性格写入请求体。
     * @return 已创建的性格定义响应。
     */
    @POST
    suspend fun create(
        @Valid request: UpsertNatureRequest,
    ): NatureResponse = service.createNature(request.toDraft()).toResponse()

    /**
     * 更新性格定义。
     *
     * @param id 性格定义主键值。
     * @param request 性格更新请求体。
     * @return 更新后的性格定义响应。
     */
    @PUT
    @Path("/{id}")
    suspend fun update(
        @PathParam("id") id: UUID,
        @Valid request: UpsertNatureRequest,
    ): NatureResponse = service.updateNature(id, request.toDraft()).toResponse()

    /**
     * 删除性格定义。
     *
     * @param id 性格定义主键值。
     * @return `204 No Content`。
     */
    @DELETE
    @Path("/{id}")
    suspend fun delete(
        @PathParam("id") id: UUID,
    ): Response {
        service.deleteNature(id)
        return Response.noContent().build()
    }
}

