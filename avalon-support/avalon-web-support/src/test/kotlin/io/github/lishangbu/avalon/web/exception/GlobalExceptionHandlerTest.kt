package io.github.lishangbu.avalon.web.exception

import io.github.lishangbu.avalon.web.result.DefaultErrorResultCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.core.MethodParameter
import org.springframework.http.HttpMethod
import org.springframework.http.converter.HttpMessageNotWritableException
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.BindException
import org.springframework.validation.FieldError
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.servlet.resource.NoResourceFoundException
import java.sql.SQLException

class GlobalExceptionHandlerTest {
    private val handler = GlobalExceptionHandler()

    @Test
    fun returnsServerErrorResponsesForGenericExceptions() {
        val global = handler.handleGlobalException(Exception("boom"))
        val runtime = handler.handleRuntimeException(RuntimeException("runtime"))
        val writable = handler.handleHttpMessageNotWritableException(HttpMessageNotWritableException("write"))

        assertEquals(DefaultErrorResultCode.SERVER_ERROR.code(), global.code)
        assertEquals("boom", global.errorMessage)
        assertEquals(DefaultErrorResultCode.SERVER_ERROR.code(), runtime.code)
        assertEquals("runtime", runtime.errorMessage)
        assertEquals(DefaultErrorResultCode.SERVER_ERROR.code(), writable.code)
        assertEquals("write", writable.errorMessage)
    }

    @Test
    fun aggregatesValidationMessagesFromMethodArgumentNotValidExceptionAndBindException() {
        val bindingResult = BeanPropertyBindingResult(ValidationPayload(""), "payload")
        bindingResult.addError(FieldError("payload", "value", "first"))
        bindingResult.addError(FieldError("payload", "value", "second"))
        val method =
            ValidationController::class.java.getDeclaredMethod(
                "submit",
                ValidationPayload::class.java,
            )
        val parameter = MethodParameter(method, 0)
        val methodArgumentException = MethodArgumentNotValidException(parameter, bindingResult)

        val methodResult = handler.handleBodyValidException(methodArgumentException)
        val bindResult = handler.handleBodyValidException(BindException(bindingResult))

        assertEquals(DefaultErrorResultCode.BAD_REQUEST.code(), methodResult.code)
        assertEquals("first,second", methodResult.errorMessage)
        assertEquals(DefaultErrorResultCode.BAD_REQUEST.code(), bindResult.code)
        assertEquals("first,second", bindResult.errorMessage)
    }

    @Test
    fun mapsSpecializedExceptionsToExpectedApiResults() {
        val illegalArgument = handler.handleIllegalArgumentException(IllegalArgumentException("bad argument"))
        val illegalState = handler.handleIllegalStateException(IllegalStateException("bad state"))
        val sql = handler.handleSQLException(SQLException("db down"))
        val unsupported = handler.handleUnsupportedOperationException(UnsupportedOperationException("unsupported"))
        val notFound =
            handler.handleNoResourceFoundException(
                NoResourceFoundException(HttpMethod.GET, "", "/missing"),
            )
        val methodNotAllowed =
            handler.handleHttpRequestMethodNotSupportedException(
                HttpRequestMethodNotSupportedException("POST"),
            )

        assertEquals(DefaultErrorResultCode.SERVER_ERROR.code(), illegalArgument.code)
        assertEquals("bad argument", illegalArgument.errorMessage)
        assertEquals(DefaultErrorResultCode.SERVER_ERROR.code(), illegalState.code)
        assertEquals("bad state", illegalState.errorMessage)
        assertEquals(DefaultErrorResultCode.SERVER_ERROR.code(), sql.code)
        assertEquals("系统开小差了，请稍后再试", sql.errorMessage)
        assertEquals(DefaultErrorResultCode.SERVER_ERROR.code(), unsupported.code)
        assertEquals("unsupported", unsupported.errorMessage)
        assertEquals(DefaultErrorResultCode.RESOURCE_NOT_FOUND.code(), notFound.code)
        assertEquals("路径[/missing]对应的资源不存在", notFound.errorMessage)
        assertEquals(DefaultErrorResultCode.RESOURCE_NOT_FOUND.code(), methodNotAllowed.code)
        assertEquals("请求方法POST不被支持", methodNotAllowed.errorMessage)
        assertNull(methodNotAllowed.data)
    }

    private data class ValidationPayload(
        val value: String,
    )

    private class ValidationController {
        @Suppress("unused")
        fun submit(payload: ValidationPayload) = payload
    }
}
