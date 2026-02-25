package io.github.lishangbu.avalon.web.exception;

import io.github.lishangbu.avalon.web.result.ApiResult;
import io.github.lishangbu.avalon.web.result.DefaultErrorResultCode;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.Assert;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/// 全局异常处理器
///
/// 统一处理控制器层抛出的各种异常并包装为标准的 ApiResult 返回
///
/// @author lishangbu
/// @since 2018/8/30
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /// 全局异常
    ///
    /// @param e 异常实例
    /// @return ApiResult 包装的错误信息
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResult<Void> handleGlobalException(Exception e) {
        log.error("全局异常信息:[{}]", e.getMessage(), e);
        return ApiResult.failed(DefaultErrorResultCode.SERVER_ERROR, e.getMessage());
    }

    /// 运行时异常
    ///
    /// @param e 异常实例
    /// @return ApiResult 包装的错误信息
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResult<Void> handleRuntimeException(RuntimeException e) {
        log.error("运行时异常信息:[{}]", e.getMessage(), e);
        return ApiResult.failed(DefaultErrorResultCode.SERVER_ERROR, e.getMessage());
    }

    /// HttpMessageNotWritableException
    ///
    /// @param e 异常实例
    /// @return ApiResult 包装的错误信息
    @ExceptionHandler(HttpMessageNotWritableException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResult<Void> handleHttpMessageNotWritableException(
            HttpMessageNotWritableException e) {
        log.error("HttpMessageNotWritableException:[{}]", e.getMessage(), e);
        return ApiResult.failed(DefaultErrorResultCode.SERVER_ERROR, e.getMessage());
    }

    /// 处理参数绑定错误（MethodArgumentNotValidException / BindException）
    ///
    /// @param exception 参数绑定异常
    /// @return ApiResult 包装的错误信息
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleBodyValidException(MethodArgumentNotValidException exception) {
        List<FieldError> fieldErrors = exception.getBindingResult().getFieldErrors();
        String errorMsg =
                fieldErrors.stream()
                        .map(DefaultMessageSourceResolvable::getDefaultMessage)
                        .collect(Collectors.joining(","));
        log.error("参数绑定异常:[{}]", errorMsg);
        return ApiResult.failed(DefaultErrorResultCode.BAD_REQUEST, errorMsg);
    }

    /// 处理业务校验过程中碰到的非法参数异常，该异常基本由 {@link Assert} 抛出
    ///
    /// @param exception 参数校验异常
    /// @return ApiResult 包装的错误信息
    /// @see Assert#hasLength(String, String)
    /// @see Assert#hasText(String, String)
    /// @see Assert#isTrue(boolean, String)
    /// @see Assert#isNull(Object, String)
    /// @see Assert#notNull(Object, String)
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResult<Void> handleIllegalArgumentException(IllegalArgumentException exception) {
        log.error("非法参数:[{}]", exception.getMessage());
        return ApiResult.failed(DefaultErrorResultCode.SERVER_ERROR, exception.getMessage());
    }

    /// 处理非法状态
    ///
    /// @param exception 异常实例
    /// @return ApiResult 包装的错误信息
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResult<Void> handleIllegalStateException(IllegalStateException exception) {
        log.error("非法状态:[{}]", exception.getMessage());
        return ApiResult.failed(DefaultErrorResultCode.SERVER_ERROR, exception.getMessage());
    }

    /// 处理 SQL 异常
    ///
    /// @param exception SQL 异常实例
    /// @return ApiResult 包装的错误信息
    @ExceptionHandler(SQLException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResult<Void> handleSQLException(SQLException exception) {
        log.error("SQL异常信息:[{}]", exception.getMessage(), exception);
        return ApiResult.failed(DefaultErrorResultCode.SERVER_ERROR, "系统开小差了，请稍后再试");
    }

    /// 不支持的操作类型
    ///
    /// @param exception 异常实例
    /// @return ApiResult 包装的错误信息
    @ExceptionHandler(UnsupportedOperationException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResult<Void> handleUnsupportedOperationException(
            UnsupportedOperationException exception) {
        log.error("不支持的操作类型:[{}]", exception.getMessage());
        return ApiResult.failed(DefaultErrorResultCode.SERVER_ERROR, exception.getMessage());
    }

    /// 处理资源不存在
    ///
    /// @param exception 资源不存在异常
    /// @return ApiResult 包装的错误信息
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResult<Void> handleNoResourceFoundException(NoResourceFoundException exception) {
        log.error(
                "资源不存在!路径:[{}],请求方法:[{}]", exception.getResourcePath(), exception.getHttpMethod());
        return ApiResult.failed(
                DefaultErrorResultCode.RESOURCE_NOT_FOUND,
                String.format("路径[%s]对应的资源不存在", exception.getResourcePath()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ApiResult<Void> handleHttpRequestMethodNotSupportedExceptionn(
            HttpRequestMethodNotSupportedException exception) {
        return ApiResult.failed(
                DefaultErrorResultCode.RESOURCE_NOT_FOUND,
                String.format("请求方法%s不被支持", exception.getMethod()));
    }
}
