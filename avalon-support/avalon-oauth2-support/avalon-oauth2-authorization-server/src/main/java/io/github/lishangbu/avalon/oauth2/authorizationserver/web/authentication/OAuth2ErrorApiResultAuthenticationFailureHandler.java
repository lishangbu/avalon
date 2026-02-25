package io.github.lishangbu.avalon.oauth2.authorizationserver.web.authentication;

import io.github.lishangbu.avalon.web.result.DefaultErrorResultCode;
import io.github.lishangbu.avalon.web.util.JsonResponseWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.http.converter.OAuth2ErrorHttpMessageConverter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

/// An implementation of an `AuthenticationFailureHandler` used for handling an
/// `OAuth2AuthenticationException` and returning the `OAuth2Error` OAuth 2.0 Error Response
///
/// @author Dmitriy Dubson
/// @author lishangbu
/// @see AuthenticationFailureHandler
/// @see OAuth2ErrorHttpMessageConverter
/// @since 2025/8/25
public class OAuth2ErrorApiResultAuthenticationFailureHandler
        implements AuthenticationFailureHandler {
    private final Log logger = LogFactory.getLog(getClass());

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authenticationException)
            throws IOException, ServletException {

        if (authenticationException
                instanceof OAuth2AuthenticationException oauth2AuthenticationException) {
            OAuth2Error error = oauth2AuthenticationException.getError();
            JsonResponseWriter.writeFailedResponse(
                    response,
                    HttpStatus.BAD_REQUEST,
                    DefaultErrorResultCode.BAD_REQUEST,
                    error.getErrorCode());
        } else {
            if (this.logger.isWarnEnabled()) {
                this.logger.warn(
                        AuthenticationException.class.getSimpleName()
                                + " must be of type "
                                + OAuth2AuthenticationException.class.getName()
                                + " but was "
                                + authenticationException.getClass().getName());
            }
        }
    }
}
