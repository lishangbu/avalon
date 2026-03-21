package io.github.lishangbu.avalon.web.exception

import io.github.lishangbu.avalon.web.result.ApiResult
import io.github.lishangbu.avalon.web.result.DefaultErrorResultCode
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotWritableException
import org.springframework.validation.BindException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.resource.NoResourceFoundException
import java.sql.SQLException

/**
 * 全局异常处理器 统一处理控制器层抛出的各种异常并包装为标准的 ApiResult 返回
 *
 * @param e 异常实例
 * @param exception 参数绑定异常 处理业务校验过程中碰到的非法参数异常，该异常基本由 [Assert] 抛出
 * @param exception 参数校验异常
 * @param exception 异常实例 处理 SQL 异常
 * @param exception SQL 异常实例 不支持的操作类型 处理资源不存在
 * @param exception 资源不存在异常
 * @return ApiResult 包装的错误信息 运行时异常 HttpMessageNotWritableException
 *   处理参数绑定错误（MethodArgumentNotValidException / BindException）
 * @see Assert#hasLength(String, String)
 * @see Assert#hasText(String, String)
 * @see Assert#isTrue(boolean, String)
 * @see Assert#isNull(Object, String)
 * @see Assert#notNull(Object, String) 处理非法状态
 * @author lishangbu
 * @since 2018/8/30 全局异常
 */

/** 全局异常处理器。 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE - 1)
class GlobalExceptionHandler {
    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleGlobalException(exception: Exception): ApiResult<Void> {
        log.error("全局异常信息:[{}]", exception.message, exception)
        return ApiResult.failed(DefaultErrorResultCode.SERVER_ERROR, exception.message.orEmpty())
    }

    @ExceptionHandler(RuntimeException::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleRuntimeException(exception: RuntimeException): ApiResult<Void> {
        log.error("运行时异常信息:[{}]", exception.message, exception)
        return ApiResult.failed(DefaultErrorResultCode.SERVER_ERROR, exception.message.orEmpty())
    }

    @ExceptionHandler(HttpMessageNotWritableException::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleHttpMessageNotWritableException(
        exception: HttpMessageNotWritableException,
    ): ApiResult<Void> {
        log.error("HttpMessageNotWritableException:[{}]", exception.message, exception)
        return ApiResult.failed(DefaultErrorResultCode.SERVER_ERROR, exception.message.orEmpty())
    }

    @ExceptionHandler(MethodArgumentNotValidException::class, BindException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleBodyValidException(exception: Exception): ApiResult<Void> {
        val fieldErrors =
            when (exception) {
                is MethodArgumentNotValidException -> exception.bindingResult.fieldErrors
                is BindException -> exception.bindingResult.fieldErrors
                else -> emptyList()
            }
        val errorMsg = fieldErrors.joinToString(",") { it.defaultMessage.orEmpty() }
        log.error("参数绑定异常:[{}]", errorMsg)
        return ApiResult.failed(DefaultErrorResultCode.BAD_REQUEST, errorMsg)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleIllegalArgumentException(exception: IllegalArgumentException): ApiResult<Void> {
        log.error("非法参数:[{}]", exception.message)
        return ApiResult.failed(DefaultErrorResultCode.SERVER_ERROR, exception.message.orEmpty())
    }

    @ExceptionHandler(IllegalStateException::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleIllegalStateException(exception: IllegalStateException): ApiResult<Void> {
        log.error("非法状态:[{}]", exception.message)
        return ApiResult.failed(DefaultErrorResultCode.SERVER_ERROR, exception.message.orEmpty())
    }

    @ExceptionHandler(SQLException::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleSQLException(exception: SQLException): ApiResult<Void> {
        log.error("SQL异常信息:[{}]", exception.message, exception)
        return ApiResult.failed(DefaultErrorResultCode.SERVER_ERROR, "系统开小差了，请稍后再试")
    }

    @ExceptionHandler(UnsupportedOperationException::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleUnsupportedOperationException(
        exception: UnsupportedOperationException,
    ): ApiResult<Void> {
        log.error("不支持的操作类型:[{}]", exception.message)
        return ApiResult.failed(DefaultErrorResultCode.SERVER_ERROR, exception.message.orEmpty())
    }

    @ExceptionHandler(NoResourceFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNoResourceFoundException(exception: NoResourceFoundException): ApiResult<Void> {
        log.error("资源不存在!路径:[{}],请求方法:[{}]", exception.resourcePath, exception.httpMethod)
        return ApiResult.failed(
            DefaultErrorResultCode.RESOURCE_NOT_FOUND,
            "路径[${exception.resourcePath}]对应的资源不存在",
        )
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    fun handleHttpRequestMethodNotSupportedException(
        exception: HttpRequestMethodNotSupportedException,
    ): ApiResult<Void> =
        ApiResult.failed(
            DefaultErrorResultCode.RESOURCE_NOT_FOUND,
            "请求方法${exception.method}不被支持",
        )

    companion object {
        private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    }
}
