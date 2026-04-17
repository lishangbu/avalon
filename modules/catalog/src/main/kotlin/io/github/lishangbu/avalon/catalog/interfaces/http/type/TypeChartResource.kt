package io.github.lishangbu.avalon.catalog.interfaces.http.type

import io.github.lishangbu.avalon.catalog.application.type.TypeChartApplicationService
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

/**
 * 属性克制矩阵维护 HTTP 入口。
 *
 * 对外以整张 `type chart` 为边界读写，避免把矩阵维护暴露成散落的行级 CRUD。
 */
@Path("/catalog/type-chart")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class TypeChartResource(
    private val service: TypeChartApplicationService,
) {
    /**
     * 读取当前属性矩阵快照。
     */
    @GET
    suspend fun get(): TypeChartResponse = service.getTypeChart().toResponse()

    /**
     * 整表替换当前属性矩阵。
     */
    @PUT
    suspend fun replace(
        @Valid request: UpsertTypeChartRequest,
    ): TypeChartResponse = service.replaceTypeChart(request.toDrafts()).toResponse()
}
