package io.github.lishangbu.avalon.catalog.interfaces.http.common.exception

import io.github.lishangbu.avalon.catalog.domain.CatalogBadRequest
import io.github.lishangbu.avalon.catalog.domain.CatalogConflict
import io.github.lishangbu.avalon.catalog.domain.CatalogNotFound
import io.github.lishangbu.avalon.shared.infra.http.problem.PROBLEM_JSON_MEDIA_TYPE
import io.github.lishangbu.avalon.shared.infra.http.problem.problemDetails
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.core.Response
import org.jboss.resteasy.reactive.server.ServerExceptionMapper

/**
 * Catalog 接口异常映射器。
 *
 * 负责把目录上下文自己的失败翻译为 RFC 9457 Problem Details，
 * 避免资源层重复处理状态码、问题类型和错误码。
 */
@ApplicationScoped
class CatalogExceptionMappers {
    /**
     * 将资源不存在映射为 `404 Not Found`。
     *
     * @param exception 资源不存在异常。
     * @return 统一的 not-found 响应。
     */
    @ServerExceptionMapper
    fun mapNotFound(exception: CatalogNotFound): Response =
        problemResponse(
            status = Response.Status.NOT_FOUND,
            problemType = "catalog/not-found",
            detail = exception.message ?: "Not found.",
            code = "catalog_not_found",
        )

    /**
     * 将目录写入冲突映射为 `409 Conflict`。
     *
     * @param exception 冲突异常。
     * @return 统一的 conflict 响应。
     */
    @ServerExceptionMapper
    fun mapConflict(exception: CatalogConflict): Response =
        problemResponse(
            status = Response.Status.CONFLICT,
            problemType = "catalog/conflict",
            detail = exception.message ?: "Conflict.",
            code = "catalog_conflict",
        )

    /**
     * 将 Catalog 自己的坏请求映射为 `400 Bad Request`。
     *
     * @param exception 坏请求异常。
     * @return 统一的 bad-request 响应。
     */
    @ServerExceptionMapper
    fun mapBadRequest(exception: CatalogBadRequest): Response {
        Log.error("Catalog request failed because of invalid arguments", exception)
        return problemResponse(
            status = Response.Status.BAD_REQUEST,
            problemType = "catalog/bad-request",
            detail = exception.message ?: "Bad request.",
            code = "catalog_bad_request",
        )
    }

    private fun problemResponse(
        status: Response.Status,
        problemType: String,
        detail: String,
        code: String,
    ): Response =
        Response.status(status)
            .type(PROBLEM_JSON_MEDIA_TYPE)
            .entity(
                problemDetails(
                    type = problemType,
                    title = status.reasonPhrase,
                    status = status.statusCode,
                    detail = detail,
                    code = code,
                ),
            ).build()
}