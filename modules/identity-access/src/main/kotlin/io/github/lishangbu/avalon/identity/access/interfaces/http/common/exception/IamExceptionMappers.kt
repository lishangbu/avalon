package io.github.lishangbu.avalon.identity.access.interfaces.http.common.exception

import io.github.lishangbu.avalon.identity.access.domain.iam.IdentityAccessBadRequest
import io.github.lishangbu.avalon.identity.access.domain.iam.IdentityAccessConflict
import io.github.lishangbu.avalon.identity.access.domain.iam.IdentityAccessNotFound
import io.github.lishangbu.avalon.shared.infra.http.problem.PROBLEM_JSON_MEDIA_TYPE
import io.github.lishangbu.avalon.shared.infra.http.problem.problemDetails
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.core.Response
import org.jboss.resteasy.reactive.server.ServerExceptionMapper

/**
 * IAM 管理接口异常映射器。
 *
 * 负责把用户、角色、权限、菜单等管理场景中的领域失败统一翻译成 RFC 9457 Problem Details。
 */
@ApplicationScoped
class IamExceptionMappers {
    /**
     * 将资源不存在映射为 `404 Not Found`。
     *
     * @param exception 资源不存在异常。
     * @return 统一的 not-found 响应。
     */
    @ServerExceptionMapper
    fun mapNotFound(exception: IdentityAccessNotFound): Response =
        problemResponse(
            status = Response.Status.NOT_FOUND,
            problemType = "identity-access/not-found",
            detail = exception.message ?: "Not found.",
            code = "identity_access_not_found",
        )

    /**
     * 将领域冲突映射为 `409 Conflict`。
     *
     * @param exception 冲突异常。
     * @return 统一的 conflict 响应。
     */
    @ServerExceptionMapper
    fun mapConflict(exception: IdentityAccessConflict): Response =
        problemResponse(
            status = Response.Status.CONFLICT,
            problemType = "identity-access/conflict",
            detail = exception.message ?: "Conflict.",
            code = "identity_access_conflict",
        )

    /**
     * 将 Identity Access 自己的坏请求映射为 `400 Bad Request`。
     *
     * @param exception 坏请求异常。
     * @return 统一的 bad-request 响应。
     */
    @ServerExceptionMapper
    fun mapBadRequest(exception: IdentityAccessBadRequest): Response {
        Log.error("IdentityAccess request failed because of invalid arguments", exception)
        return problemResponse(
            status = Response.Status.BAD_REQUEST,
            problemType = "identity-access/bad-request",
            detail = exception.message ?: "Bad request.",
            code = "identity_access_bad_request",
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