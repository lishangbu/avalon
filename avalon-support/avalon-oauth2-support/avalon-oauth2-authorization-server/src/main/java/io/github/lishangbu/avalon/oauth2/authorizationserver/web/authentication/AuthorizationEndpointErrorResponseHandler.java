package io.github.lishangbu.avalon.oauth2.authorizationserver.web.authentication;

import io.github.lishangbu.avalon.oauth2.common.result.SecurityErrorResultCode;
import io.github.lishangbu.avalon.web.util.JsonResponseWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import tools.jackson.databind.json.JsonMapper;

/// An implementation of an
// [org.springframework.security.web.authentication.AuthenticationFailureHandler]
/// used for handling an [org.springframework.security.oauth2.core.OAuth2AuthenticationException]
/// and returning the [org.springframework.security.oauth2.core.OAuth2Error]
/// OAuth 2.0 Error Response
///
/// @author Dmitriy Dubson
/// @author lishangbu
/// @see org.springframework.security.web.authentication.AuthenticationFailureHandler
/// @see org.springframework.security.oauth2.core.http.converter.OAuth2ErrorHttpMessageConverter
/// @since 1.2
/// @since 2025/8/25
public class AuthorizationEndpointErrorResponseHandler implements AuthenticationFailureHandler {

    private final JsonMapper jsonMapper;

    public AuthorizationEndpointErrorResponseHandler(JsonMapper jsonMapper) {
        this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper");
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception)
            throws IOException, ServletException {
        String errorMessage = exception.getMessage();
        if (errorMessage == null && exception instanceof OAuth2AuthenticationException oauth2Exception) {
            OAuth2Error error = oauth2Exception.getError();
            if (error != null) {
                errorMessage = error.getDescription();
                if (errorMessage == null) {
                    errorMessage = error.getErrorCode();
                }
            }
        }
        JsonResponseWriter.writeFailedResponse(
                response,
                jsonMapper,
                HttpStatus.UNAUTHORIZED,
                SecurityErrorResultCode.UNAUTHORIZED,
                errorMessage);
    }
}
