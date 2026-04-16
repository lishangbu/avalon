package io.github.lishangbu.avalon.app.interfaces.http.common.exception

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.databind.JsonMappingException
import io.github.lishangbu.avalon.shared.infra.http.problem.PROBLEM_JSON_MEDIA_TYPE
import io.github.lishangbu.avalon.shared.infra.http.problem.problemDetails
import io.github.lishangbu.avalon.shared.infra.http.problem.ProblemDetailsResponse
import io.github.lishangbu.avalon.shared.infra.http.problem.ProblemFieldError
import io.quarkus.logging.Log
import io.quarkus.security.ForbiddenException as QuarkusForbiddenException
import io.quarkus.security.UnauthorizedException as QuarkusUnauthorizedException
import jakarta.annotation.Priority
import jakarta.enterprise.context.ApplicationScoped
import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import jakarta.ws.rs.*
import jakarta.ws.rs.core.Response
import org.jboss.resteasy.reactive.server.ServerExceptionMapper

/**
 * 全局 Problem Details 异常映射器。
 *
 * 这里负责把框架级、协议级和兜底异常统一转换为 RFC 9457 Problem Details，
 * 让各 bounded context 之外的失败也能维持稳定的错误响应结构。
 */
@ApplicationScoped
@Priority(Priorities.USER - 100)
class GlobalProblemExceptionMappers {
    /**
     * 将 Bean Validation 失败统一映射为 `400 Bad Request`。
     *
     * @param exception 参数或请求体验证失败异常。
     * @return 带字段级错误明细的 Problem Details 响应。
     */
    @ServerExceptionMapper
    fun mapConstraintViolation(exception: ConstraintViolationException): Response =
        problemResponse(
            status = Response.Status.BAD_REQUEST,
            problem =
                problemDetails(
                    type = "request/validation-failed",
                    title = Response.Status.BAD_REQUEST.reasonPhrase,
                    status = Response.Status.BAD_REQUEST.statusCode,
                    detail = "Request validation failed.",
                    code = "request_validation_failed",
                    errors = exception.constraintViolations.sortedBy { it.propertyPath.toString() }
                        .map { it.toProblemFieldError() },
                ),
        )

    /**
     * 将 JSON 语法错误统一映射为 `400 Bad Request`。
     *
     * @param exception 请求体 JSON 语法错误异常。
     * @return 统一的 malformed-json Problem Details。
     */
    @ServerExceptionMapper
    fun mapJsonParseException(exception: JsonParseException): Response =
        problemResponse(
            status = Response.Status.BAD_REQUEST,
            problem =
                problemDetails(
                    type = "request/body-malformed",
                    title = Response.Status.BAD_REQUEST.reasonPhrase,
                    status = Response.Status.BAD_REQUEST.statusCode,
                    detail = "Request body contains malformed JSON.",
                    code = "request_body_malformed",
                ),
        )

    /**
     * 将 Jackson 的字段类型不匹配统一映射为 `400 Bad Request`。
     *
     * 这里单独声明 `MismatchedInputException`，是为了覆盖框架默认的 Jackson
     * 错误响应，避免返回 RESTEasy Reactive 自带的结构化错误体。
     *
     * @param exception 请求体字段类型或枚举值不匹配异常。
     * @return 统一的 invalid-body Problem Details。
     */
    @ServerExceptionMapper
    fun mapMismatchedInputException(exception: MismatchedInputException): Response =
        problemResponse(
            status = Response.Status.BAD_REQUEST,
            problem =
                problemDetails(
                    type = "request/body-invalid",
                    title = Response.Status.BAD_REQUEST.reasonPhrase,
                    status = Response.Status.BAD_REQUEST.statusCode,
                    detail = "Request body contains invalid or incompatible values.",
                    code = "request_body_invalid",
                ),
        )

    /**
     * 将 JSON 字段类型或结构不匹配统一映射为 `400 Bad Request`。
     *
     * @param exception 请求体反序列化异常。
     * @return 统一的 invalid-body Problem Details。
     */
    @ServerExceptionMapper
    fun mapJsonMappingException(exception: JsonMappingException): Response =
        problemResponse(
            status = Response.Status.BAD_REQUEST,
            problem =
                problemDetails(
                    type = "request/body-invalid",
                    title = Response.Status.BAD_REQUEST.reasonPhrase,
                    status = Response.Status.BAD_REQUEST.statusCode,
                    detail = "Request body contains invalid or incompatible values.",
                    code = "request_body_invalid",
                ),
        )

    /**
     * 将剩余的 `400 Bad Request` 协议级失败统一收口。
     *
     * @param exception 通用坏请求异常。
     * @return RFC 9457 Problem Details 响应。
     */
    @ServerExceptionMapper
    fun mapBadRequest(exception: BadRequestException): Response =
        problemResponse(
            status = Response.Status.BAD_REQUEST,
            problem = problemForBadRequest(exception),
        )

    /**
     * 将未被更具体 mapper 捕获的 `WebApplicationException` 统一收口。
     *
     * 当前重点是把 Jackson 反序列化阶段抛出的 `400 Bad Request`
     * 也转换为 Problem Details，而不是落入兜底 `500`。
     *
     * @param exception 协议层 WebApplication 异常。
     * @return RFC 9457 Problem Details 响应。
     */
    @ServerExceptionMapper
    fun mapWebApplicationException(exception: WebApplicationException): Response =
        when (exception.response?.status) {
            Response.Status.BAD_REQUEST.statusCode ->
                problemResponse(
                    status = Response.Status.BAD_REQUEST,
                    problem = problemForBadRequest(exception),
                )

            else -> throw exception
        }

    /**
     * 将框架级未授权访问统一映射为 `401 Unauthorized`。
     *
     * @param exception JAX-RS 未授权异常。
     * @return 统一的 unauthorized Problem Details。
     */
    @ServerExceptionMapper
    fun mapNotAuthorized(exception: NotAuthorizedException): Response =
        problemResponse(
            status = Response.Status.UNAUTHORIZED,
            problem =
                problemDetails(
                    type = "security/unauthorized",
                    title = Response.Status.UNAUTHORIZED.reasonPhrase,
                    status = Response.Status.UNAUTHORIZED.statusCode,
                    detail = "Authentication is required to access this resource.",
                    code = "security_unauthorized",
                ),
        )

    /**
     * 将 Quarkus 安全层抛出的未授权访问统一映射为 `401 Unauthorized`。
     *
     * @param exception Quarkus 安全未授权异常。
     * @return 统一的 unauthorized Problem Details。
     */
    @ServerExceptionMapper
    fun mapQuarkusUnauthorized(exception: QuarkusUnauthorizedException): Response =
        problemResponse(
            status = Response.Status.UNAUTHORIZED,
            problem =
                problemDetails(
                    type = "security/unauthorized",
                    title = Response.Status.UNAUTHORIZED.reasonPhrase,
                    status = Response.Status.UNAUTHORIZED.statusCode,
                    detail = "Authentication is required to access this resource.",
                    code = "security_unauthorized",
                ),
        )

    /**
     * 将框架级禁止访问统一映射为 `403 Forbidden`。
     *
     * @param exception JAX-RS 禁止访问异常。
     * @return 统一的 forbidden Problem Details。
     */
    @ServerExceptionMapper
    fun mapForbidden(exception: ForbiddenException): Response =
        problemResponse(
            status = Response.Status.FORBIDDEN,
            problem =
                problemDetails(
                    type = "security/forbidden",
                    title = Response.Status.FORBIDDEN.reasonPhrase,
                    status = Response.Status.FORBIDDEN.statusCode,
                    detail = "You do not have permission to access this resource.",
                    code = "security_forbidden",
                ),
        )

    /**
     * 将 Quarkus 安全层抛出的禁止访问统一映射为 `403 Forbidden`。
     *
     * @param exception Quarkus 安全禁止访问异常。
     * @return 统一的 forbidden Problem Details。
     */
    @ServerExceptionMapper
    fun mapQuarkusForbidden(exception: QuarkusForbiddenException): Response =
        problemResponse(
            status = Response.Status.FORBIDDEN,
            problem =
                problemDetails(
                    type = "security/forbidden",
                    title = Response.Status.FORBIDDEN.reasonPhrase,
                    status = Response.Status.FORBIDDEN.statusCode,
                    detail = "You do not have permission to access this resource.",
                    code = "security_forbidden",
                ),
        )

    /**
     * 将未命中路由统一映射为 `404 Not Found`。
     *
     * @param exception 路由未命中异常。
     * @return 统一的 not-found Problem Details。
     */
    @ServerExceptionMapper
    fun mapNotFound(exception: NotFoundException): Response =
        problemResponse(
            status = Response.Status.NOT_FOUND,
            problem =
                problemDetails(
                    type = "http/not-found",
                    title = Response.Status.NOT_FOUND.reasonPhrase,
                    status = Response.Status.NOT_FOUND.statusCode,
                    detail = "The requested resource was not found.",
                    code = "http_not_found",
                ),
        )

    /**
     * 将不允许的 HTTP 方法统一映射为 `405 Method Not Allowed`。
     *
     * @param exception 方法不允许异常。
     * @return 统一的 method-not-allowed Problem Details。
     */
    @ServerExceptionMapper
    fun mapNotAllowed(exception: NotAllowedException): Response =
        problemResponse(
            status = Response.Status.METHOD_NOT_ALLOWED,
            problem =
                problemDetails(
                    type = "http/method-not-allowed",
                    title = Response.Status.METHOD_NOT_ALLOWED.reasonPhrase,
                    status = Response.Status.METHOD_NOT_ALLOWED.statusCode,
                    detail = "The requested HTTP method is not allowed for this resource.",
                    code = "http_method_not_allowed",
                ),
        )

    /**
     * 将不支持的媒体类型统一映射为 `415 Unsupported Media Type`。
     *
     * @param exception 媒体类型不支持异常。
     * @return 统一的 unsupported-media-type Problem Details。
     */
    @ServerExceptionMapper
    fun mapNotSupported(exception: NotSupportedException): Response =
        problemResponse(
            status = Response.Status.UNSUPPORTED_MEDIA_TYPE,
            problem =
                problemDetails(
                    type = "http/unsupported-media-type",
                    title = Response.Status.UNSUPPORTED_MEDIA_TYPE.reasonPhrase,
                    status = Response.Status.UNSUPPORTED_MEDIA_TYPE.statusCode,
                    detail = "The request media type is not supported.",
                    code = "http_unsupported_media_type",
                ),
        )

    /**
     * 将未捕获异常统一映射为 `500 Internal Server Error`。
     *
     * @param exception 未预期异常。
     * @return 不暴露实现细节的 Problem Details 响应。
     */
    @ServerExceptionMapper
    fun mapThrowable(exception: Throwable): Response {
        Log.error("Unhandled server error", exception)
        return problemResponse(
            status = Response.Status.INTERNAL_SERVER_ERROR,
            problem =
                problemDetails(
                    type = "internal/unexpected-error",
                    title = Response.Status.INTERNAL_SERVER_ERROR.reasonPhrase,
                    status = Response.Status.INTERNAL_SERVER_ERROR.statusCode,
                    detail = "The server failed to process the request.",
                    code = "internal_server_error",
                ),
        )
    }

    private fun problemForBadRequest(exception: Throwable): ProblemDetailsResponse {
        val rootCause = rootCauseOf(exception)
        return when (rootCause) {
            is JsonParseException ->
                problemDetails(
                    type = "request/body-malformed",
                    title = Response.Status.BAD_REQUEST.reasonPhrase,
                    status = Response.Status.BAD_REQUEST.statusCode,
                    detail = "Request body contains malformed JSON.",
                    code = "request_body_malformed",
                )

            is MismatchedInputException, is JsonMappingException ->
                problemDetails(
                    type = "request/body-invalid",
                    title = Response.Status.BAD_REQUEST.reasonPhrase,
                    status = Response.Status.BAD_REQUEST.statusCode,
                    detail = "Request body contains invalid or incompatible values.",
                    code = "request_body_invalid",
                )

            else ->
                problemDetails(
                    type = "request/bad-request",
                    title = Response.Status.BAD_REQUEST.reasonPhrase,
                    status = Response.Status.BAD_REQUEST.statusCode,
                    detail = exception.message?.takeIf { it.isNotBlank() } ?: "Bad request.",
                    code = "request_bad_request",
                )
        }
    }

    private fun problemResponse(
        status: Response.Status,
        problem: ProblemDetailsResponse,
    ): Response =
        Response.status(status)
            .type(PROBLEM_JSON_MEDIA_TYPE)
            .entity(problem)
            .build()

    private fun ConstraintViolation<*>.toProblemFieldError(): ProblemFieldError =
        ProblemFieldError(
            field = propertyPath.toFieldName(),
            reason = message,
        )

    private fun rootCauseOf(exception: Throwable): Throwable {
        var current = exception
        while (current.cause != null && current.cause !== current) {
            current = current.cause!!
        }
        return current
    }

    private fun jakarta.validation.Path.toFieldName(): String =
        iterator().asSequence().mapNotNull { it.name }.lastOrNull() ?: toString()
}