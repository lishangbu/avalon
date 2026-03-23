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
 * 全局异常处理器
 *
 * 统一处理控制器抛出的异常并返回标准化错误响应
 *
 * @author lishangbu
 * @since 2018/8/30
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE - 1)
class GlobalExceptionHandler {
    /** 处理全局异常 */
    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleGlobalException(exception: Exception): ApiResult<Void> {
        log.error("全局异常信息:[{}]", exception.message, exception)
        return ApiResult.failed(DefaultErrorResultCode.SERVER_ERROR, exception.message.orEmpty())
    }

    /** 处理运行时异常 */
    @ExceptionHandler(RuntimeException::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleRuntimeException(exception: RuntimeException): ApiResult<Void> {
        log.error("运行时异常信息:[{}]", exception.message, exception)
        return ApiResult.failed(DefaultErrorResultCode.SERVER_ERROR, exception.message.orEmpty())
    }

    /** 处理HTTP 消息不可写异常 */
    @ExceptionHandler(HttpMessageNotWritableException::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleHttpMessageNotWritableException(
        exception: HttpMessageNotWritableException,
    ): ApiResult<Void> {
        log.error("HttpMessageNotWritableException:[{}]", exception.message, exception)
        return ApiResult.failed(DefaultErrorResultCode.SERVER_ERROR, exception.message.orEmpty())
    }

    /** 处理请求体验证异常 */
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

    /** 处理非法参数异常 */
    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleIllegalArgumentException(exception: IllegalArgumentException): ApiResult<Void> {
        log.error("非法参数:[{}]", exception.message)
        return ApiResult.failed(DefaultErrorResultCode.SERVER_ERROR, exception.message.orEmpty())
    }

    /** 处理非法状态异常 */
    @ExceptionHandler(IllegalStateException::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleIllegalStateException(exception: IllegalStateException): ApiResult<Void> {
        log.error("非法状态:[{}]", exception.message)
        return ApiResult.failed(DefaultErrorResultCode.SERVER_ERROR, exception.message.orEmpty())
    }

    /** 处理SQL 异常 */
    @ExceptionHandler(SQLException::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleSQLException(exception: SQLException): ApiResult<Void> {
        log.error("SQL异常信息:[{}]", exception.message, exception)
        return ApiResult.failed(DefaultErrorResultCode.SERVER_ERROR, "系统开小差了，请稍后再试")
    }

    /** 处理不支持操作异常 */
    @ExceptionHandler(UnsupportedOperationException::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleUnsupportedOperationException(
        exception: UnsupportedOperationException,
    ): ApiResult<Void> {
        log.error("不支持的操作类型:[{}]", exception.message)
        return ApiResult.failed(DefaultErrorResultCode.SERVER_ERROR, exception.message.orEmpty())
    }

    /** 处理资源不存在异常 */
    @ExceptionHandler(NoResourceFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNoResourceFoundException(exception: NoResourceFoundException): ApiResult<Void> {
        log.error("资源不存在!路径:[{}],请求方法:[{}]", exception.resourcePath, exception.httpMethod)
        return ApiResult.failed(
            DefaultErrorResultCode.RESOURCE_NOT_FOUND,
            "路径[${exception.resourcePath}]对应的资源不存在",
        )
    }

    /** 处理HTTP 请求方法不支持异常 */
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
        /** 日志记录器 */
        private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    }
}
