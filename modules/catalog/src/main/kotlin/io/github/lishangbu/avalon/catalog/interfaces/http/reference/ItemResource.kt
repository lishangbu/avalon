package io.github.lishangbu.avalon.catalog.interfaces.http.reference

import io.github.lishangbu.avalon.catalog.application.CatalogService
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.util.UUID

/**
 * 道具定义维护 HTTP 入口。
 */
@Path("/catalog/items")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class ItemResource(
    private val service: CatalogService,
) {
    /**
     * 列出全部道具定义。
     *
     * @return 道具定义响应列表。
     */
    @GET
    suspend fun list(): List<ItemResponse> = service.listItems().map { it.toResponse() }

    /**
     * 查询单个道具定义。
     *
     * @param id 道具定义主键值。
     * @return 命中的道具定义响应。
     */
    @GET
    @Path("/{id}")
    suspend fun getById(
        @PathParam("id") id: UUID,
    ): ItemResponse = service.getItem(id).toResponse()

    /**
     * 创建道具定义。
     *
     * @param request 道具写入请求体。
     * @return 已创建的道具定义响应。
     */
    @POST
    suspend fun create(
        @Valid request: UpsertItemRequest,
    ): ItemResponse = service.createItem(request.toDraft()).toResponse()

    /**
     * 更新道具定义。
     *
     * @param id 道具定义主键值。
     * @param request 道具更新请求体。
     * @return 更新后的道具定义响应。
     */
    @PUT
    @Path("/{id}")
    suspend fun update(
        @PathParam("id") id: UUID,
        @Valid request: UpsertItemRequest,
    ): ItemResponse = service.updateItem(id, request.toDraft()).toResponse()

    /**
     * 删除道具定义。
     *
     * @param id 道具定义主键值。
     * @return `204 No Content`。
     */
    @DELETE
    @Path("/{id}")
    suspend fun delete(
        @PathParam("id") id: UUID,
    ): Response {
        service.deleteItem(id)
        return Response.noContent().build()
    }
}

