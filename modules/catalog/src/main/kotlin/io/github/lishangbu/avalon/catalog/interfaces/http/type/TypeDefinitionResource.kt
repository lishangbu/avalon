package io.github.lishangbu.avalon.catalog.interfaces.http.type

import io.github.lishangbu.avalon.catalog.application.type.TypeDefinitionApplicationService
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.util.UUID

/**
 * 属性定义维护 HTTP 入口。
 */
@Path("/catalog/types")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class TypeDefinitionResource(
    private val service: TypeDefinitionApplicationService,
) {
    /**
     * 列出全部属性定义。
     */
    @GET
    suspend fun list(): List<TypeDefinitionResponse> = service.listTypeDefinitions().map { it.toResponse() }

    /**
     * 查询单个属性定义。
     */
    @GET
    @Path("/{id}")
    suspend fun getById(
        @PathParam("id") id: UUID,
    ): TypeDefinitionResponse = service.getTypeDefinition(id).toResponse()

    /**
     * 创建属性定义。
     */
    @POST
    suspend fun create(
        @Valid request: UpsertTypeDefinitionRequest,
    ): TypeDefinitionResponse = service.createTypeDefinition(request.toDraft()).toResponse()

    /**
     * 更新属性定义。
     */
    @PUT
    @Path("/{id}")
    suspend fun update(
        @PathParam("id") id: UUID,
        @Valid request: UpsertTypeDefinitionRequest,
    ): TypeDefinitionResponse = service.updateTypeDefinition(id, request.toDraft()).toResponse()

    /**
     * 删除属性定义。
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
