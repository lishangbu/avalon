package io.github.lishangbu.avalon.catalog.interfaces.http.move

import io.github.lishangbu.avalon.catalog.application.CatalogService
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.util.UUID

/**
 * 招式分类定义的 HTTP 入口。
 */
@Path("/catalog/move-categories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class MoveCategoryResource(
    private val service: CatalogService,
) {
    /**
     * 列出全部招式分类定义。
     */
    @GET
    suspend fun list(): List<MoveCategoryResponse> = service.listMoveCategories().map { it.toResponse() }

    /**
     * 查询单个招式分类定义。
     */
    @GET
    @Path("/{id}")
    suspend fun getById(
        @PathParam("id") id: UUID,
    ): MoveCategoryResponse = service.getMoveCategory(id).toResponse()

    /**
     * 创建招式分类定义。
     */
    @POST
    suspend fun create(
        @Valid request: UpsertMoveCategoryRequest,
    ): MoveCategoryResponse = service.createMoveCategory(request.toDraft()).toResponse()

    /**
     * 更新招式分类定义。
     */
    @PUT
    @Path("/{id}")
    suspend fun update(
        @PathParam("id") id: UUID,
        @Valid request: UpsertMoveCategoryRequest,
    ): MoveCategoryResponse = service.updateMoveCategory(id, request.toDraft()).toResponse()

    /**
     * 删除招式分类定义。
     */
    @DELETE
    @Path("/{id}")
    suspend fun delete(
        @PathParam("id") id: UUID,
    ): Response {
        service.deleteMoveCategory(id)
        return Response.noContent().build()
    }
}

