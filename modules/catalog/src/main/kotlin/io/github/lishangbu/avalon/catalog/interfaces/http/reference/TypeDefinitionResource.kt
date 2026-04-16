package io.github.lishangbu.avalon.catalog.interfaces.http.reference

import io.github.lishangbu.avalon.catalog.application.CatalogService
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.util.UUID

/**
 * 属性定义维护 HTTP 入口。
 */
@Path("/api/catalog/types")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class TypeDefinitionResource(
    private val service: CatalogService,
) {
    /**
     * 列出全部属性定义。
     *
     * @return 属性定义响应列表。
     */
    @GET
    suspend fun list(): List<TypeDefinitionResponse> = service.listTypeDefinitions().map { it.toResponse() }

    /**
     * 查询单个属性定义。
     *
     * @param id 属性定义主键值。
     * @return 命中的属性定义响应。
     */
    @GET
    @Path("/{id}")
    suspend fun getById(
        @PathParam("id") id: UUID,
    ): TypeDefinitionResponse = service.getTypeDefinition(id).toResponse()

    /**
     * 创建属性定义。
     *
     * @param request 属性定义写入请求体。
     * @return 已创建的属性定义响应。
     */
    @POST
    suspend fun create(
        @Valid request: UpsertTypeDefinitionRequest,
    ): TypeDefinitionResponse = service.createTypeDefinition(request.toDraft()).toResponse()

    /**
     * 更新属性定义。
     *
     * @param id 属性定义主键值。
     * @param request 属性定义更新请求体。
     * @return 更新后的属性定义响应。
     */
    @PUT
    @Path("/{id}")
    suspend fun update(
        @PathParam("id") id: UUID,
        @Valid request: UpsertTypeDefinitionRequest,
    ): TypeDefinitionResponse = service.updateTypeDefinition(id, request.toDraft()).toResponse()

    /**
     * 删除属性定义。
     *
     * @param id 属性定义主键值。
     * @return `204 No Content`。
     */
    @DELETE
    @Path("/{id}")
    suspend fun delete(
        @PathParam("id") id: UUID,
    ): Response {
        service.deleteTypeDefinition(id)
        return Response.noContent().build()
    }
}

