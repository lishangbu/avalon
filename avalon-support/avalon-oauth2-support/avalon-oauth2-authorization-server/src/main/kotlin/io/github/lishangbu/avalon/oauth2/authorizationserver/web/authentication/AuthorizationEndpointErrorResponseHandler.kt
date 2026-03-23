package io.github.lishangbu.avalon.oauth2.authorizationserver.web.authentication

import io.github.lishangbu.avalon.oauth2.common.result.SecurityErrorResultCode
import io.github.lishangbu.avalon.web.util.JsonResponseWriter
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import tools.jackson.databind.json.JsonMapper
import java.io.IOException

/** An implementation of an */
// [org.springframework.security.web.authentication.AuthenticationFailureHandler]

/**
 * used for handling an [org.springframework.security.oauth2.core.OAuth2AuthenticationException] and
 * returning the [org.springframework.security.oauth2.core.OAuth2Error] OAuth 2.0 Error Response
 *
 * @see org.springframework.security.web.authentication.AuthenticationFailureHandler
 * @see org.springframework.security.oauth2.core.http.converter.OAuth2ErrorHttpMessageConverter
 * @author Dmitriy Dubson
 * @author lishangbu
 * @since 1.2
 * @since 2025/8/25
 */
class AuthorizationEndpointErrorResponseHandler(
    jsonMapper: JsonMapper,
) : AuthenticationFailureHandler {
    private val jsonMapper = jsonMapper

    @Throws(IOException::class, ServletException::class)
    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException,
    ) {
        var errorMessage = exception.message
        if (errorMessage == null && exception is OAuth2AuthenticationException) {
            val error: OAuth2Error? = exception.error
            if (error != null) {
                errorMessage = error.description
                if (errorMessage == null) {
                    errorMessage = error.errorCode
                }
            }
        }
        JsonResponseWriter.writeFailedResponse(
            response,
            jsonMapper,
            HttpStatus.UNAUTHORIZED,
            SecurityErrorResultCode.UNAUTHORIZED,
            errorMessage.orEmpty(),
        )
    }
}
