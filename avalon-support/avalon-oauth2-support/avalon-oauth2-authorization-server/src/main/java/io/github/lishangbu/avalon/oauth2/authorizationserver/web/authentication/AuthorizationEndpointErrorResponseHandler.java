package io.github.lishangbu.avalon.oauth2.authorizationserver.web.authentication;

import io.github.lishangbu.avalon.oauth2.common.result.SecurityErrorResultCode;
import io.github.lishangbu.avalon.web.util.JsonResponseWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

/// An implementation of an {@link
// org.springframework.security.web.authentication.AuthenticationFailureHandler}
/// used for handling an {@link
// org.springframework.security.oauth2.core.OAuth2AuthenticationException}
/// and returning the {@link org.springframework.security.oauth2.core.OAuth2Error OAuth 2.0 Error
// Response}

///
/// @author Dmitriy Dubson
/// @author lishangbu
/// @see org.springframework.security.web.authentication.AuthenticationFailureHandler
/// @see org.springframework.security.oauth2.core.http.converter.OAuth2ErrorHttpMessageConverter
/// @since 1.2
/// @since 2025/8/25
public class AuthorizationEndpointErrorResponseHandler implements AuthenticationFailureHandler {
  @Override
  public void onAuthenticationFailure(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
      throws IOException, ServletException {
    JsonResponseWriter.writeFailedResponse(
        response,
        HttpStatus.UNAUTHORIZED,
        SecurityErrorResultCode.UNAUTHORIZED,
        exception.getMessage());
  }
}
