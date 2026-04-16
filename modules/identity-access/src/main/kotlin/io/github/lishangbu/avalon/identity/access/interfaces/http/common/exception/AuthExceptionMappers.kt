package io.github.lishangbu.avalon.identity.access.interfaces.http.common.exception

import io.github.lishangbu.avalon.identity.access.domain.authentication.AuthenticationFailed
import io.github.lishangbu.avalon.identity.access.domain.authentication.AuthenticationSessionNotFound
import io.github.lishangbu.avalon.identity.access.domain.authentication.AuthenticationUnauthorized
import io.github.lishangbu.avalon.shared.infra.http.problem.PROBLEM_JSON_MEDIA_TYPE
import io.github.lishangbu.avalon.shared.infra.http.problem.problemDetails
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.core.Response
import org.jboss.resteasy.reactive.server.ServerExceptionMapper

/**
 * 认证接口异常映射器。
 *
 * 负责把认证与会话相关的领域失败统一转换为 RFC 9457 Problem Details，
 * 避免资源层重复拼装问题类型、错误码和状态码。
 */
@ApplicationScoped
class AuthExceptionMappers {
    /**
     * 把认证失败映射为 `401 Unauthorized`。
     *
     * @param exception 认证失败异常。
     * @return 统一的认证失败响应。
     */
    @ServerExceptionMapper
    fun mapAuthenticationFailed(exception: AuthenticationFailed): Response =
        problemResponse(
            status = Response.Status.UNAUTHORIZED,
            problemType = "authentication/failed",
            detail = exception.message ?: "Authentication failed.",
            code = "authentication_failed",
        )

    /**
     * 把会话不存在映射为 `404 Not Found`。
     *
     * @param exception 会话不存在异常。
     * @return 统一的会话不存在响应。
     */
    @ServerExceptionMapper
    fun mapAuthenticationSessionNotFound(exception: AuthenticationSessionNotFound): Response =
        problemResponse(
            status = Response.Status.NOT_FOUND,
            problemType = "authentication/session-not-found",
            detail = exception.message ?: "Session not found.",
            code = "authentication_session_not_found",
        )

    /**
     * 把未授权访问映射为 `401 Unauthorized`。
     *
     * @param exception 未授权异常。
     * @return 统一的未授权响应。
     */
    @ServerExceptionMapper
    fun mapAuthenticationUnauthorized(exception: AuthenticationUnauthorized): Response =
        problemResponse(
            status = Response.Status.UNAUTHORIZED,
            problemType = "authentication/unauthorized",
            detail = exception.message ?: "Unauthorized.",
            code = "authentication_unauthorized",
        )

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