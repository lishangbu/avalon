package io.github.lishangbu.avalon.oauth2.authorizationserver.web.authentication;

import io.github.lishangbu.avalon.oauth2.common.log.AuthenticationLogRecord;
import io.github.lishangbu.avalon.oauth2.common.log.AuthenticationLogRecorder;
import io.github.lishangbu.avalon.oauth2.common.properties.Oauth2Properties;
import io.github.lishangbu.avalon.web.result.DefaultErrorResultCode;
import io.github.lishangbu.avalon.web.util.JsonResponseWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import tools.jackson.databind.json.JsonMapper;

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
    private final AuthenticationLogRecorder authenticationLogRecorder;
    private final Oauth2Properties oauth2Properties;
    private final JsonMapper jsonMapper;

    public OAuth2ErrorApiResultAuthenticationFailureHandler(JsonMapper jsonMapper) {
        this(AuthenticationLogRecorder.noop(), null, jsonMapper);
    }

    public OAuth2ErrorApiResultAuthenticationFailureHandler(
            AuthenticationLogRecorder authenticationLogRecorder, JsonMapper jsonMapper) {
        this(authenticationLogRecorder, null, jsonMapper);
    }

    public OAuth2ErrorApiResultAuthenticationFailureHandler(
            AuthenticationLogRecorder authenticationLogRecorder,
            Oauth2Properties oauth2Properties,
            JsonMapper jsonMapper) {
        this.authenticationLogRecorder =
                authenticationLogRecorder == null
                        ? AuthenticationLogRecorder.noop()
                        : authenticationLogRecorder;
        this.oauth2Properties = oauth2Properties;
        this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper");
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authenticationException)
            throws IOException, ServletException {

        ResolvedError resolvedError = resolveError(authenticationException);
        recordAuthenticationFailure(request, resolvedError);
        writeFailedResponse(
                response, normalize(resolvedError.code()), normalize(resolvedError.description()));
        if (!(authenticationException instanceof OAuth2AuthenticationException)
                && this.logger.isWarnEnabled()) {
            this.logger.warn(
                    AuthenticationException.class.getSimpleName()
                            + " must be of type "
                            + OAuth2AuthenticationException.class.getName()
                            + " but was "
                            + authenticationException.getClass().getName());
        }
    }

    private ResolvedError resolveError(AuthenticationException authenticationException) {
        if (authenticationException
                instanceof OAuth2AuthenticationException oauth2AuthenticationException) {
            OAuth2Error error = oauth2AuthenticationException.getError();
            String errorCode = error == null ? null : error.getErrorCode();
            String errorDescription = error == null ? null : error.getDescription();
            if (normalize(errorDescription) == null) {
                String message = normalize(oauth2AuthenticationException.getMessage());
                if (message != null && !message.equalsIgnoreCase(normalize(errorCode))) {
                    errorDescription = message;
                }
            }
            return new ResolvedError(errorCode, sanitizeDescription(errorDescription));
        }
        if (authenticationException == null) {
            return new ResolvedError(null, null);
        }
        return new ResolvedError(
                authenticationException.getClass().getSimpleName(),
                sanitizeDescription(authenticationException.getMessage()));
    }

    private void recordAuthenticationFailure(HttpServletRequest request, ResolvedError error) {
        try {
            String errorMessage = sanitizeDescription(error.description());
            if (errorMessage == null) {
                String code = normalize(error.code());
                if (code != null && !OAuth2ErrorCodes.INVALID_GRANT.equals(code)) {
                    errorMessage = code;
                }
            }
            AuthenticationLogRecord record =
                    new AuthenticationLogRecord(
                            normalize(request.getParameter(resolveUsernameParameterName())),
                            resolveClientId(request),
                            normalize(request.getParameter("grant_type")),
                            resolveClientIp(request),
                            normalize(request.getHeader("User-Agent")),
                            false,
                            errorMessage,
                            Instant.now());
            authenticationLogRecorder.record(record);
        } catch (Exception ex) {
            if (this.logger.isWarnEnabled()) {
                this.logger.warn("Failed to record authentication log", ex);
            }
        }
    }

    private String resolveUsernameParameterName() {
        if (oauth2Properties == null) {
            return "username";
        }
        String configured = oauth2Properties.getUsernameParameterName();
        return normalize(configured) == null ? "username" : configured;
    }

    private String resolveClientId(HttpServletRequest request) {
        String clientId = normalize(request.getParameter("client_id"));
        if (clientId != null) {
            return clientId;
        }
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Basic ")) {
            return null;
        }
        String base64Credentials = authorization.substring("Basic ".length()).trim();
        if (base64Credentials.isEmpty()) {
            return null;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(base64Credentials);
            String credentials = new String(decoded, StandardCharsets.UTF_8);
            int delimiterIndex = credentials.indexOf(':');
            if (delimiterIndex > 0) {
                return normalize(credentials.substring(0, delimiterIndex));
            }
        } catch (IllegalArgumentException ex) {
            if (this.logger.isTraceEnabled()) {
                this.logger.trace("Failed to decode client credentials", ex);
            }
        }
        return null;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String[] parts = forwardedFor.split(",", 2);
            return normalize(parts[0].trim());
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return normalize(realIp.trim());
        }
        return normalize(request.getRemoteAddr());
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void writeFailedResponse(
            HttpServletResponse response, String errorCode, String errorDescription) {
        String code = normalize(errorCode);
        String description = sanitizeDescription(errorDescription);
        if (description != null) {
            JsonResponseWriter.writeFailedResponse(
                    response,
                    jsonMapper,
                    HttpStatus.BAD_REQUEST,
                    DefaultErrorResultCode.BAD_REQUEST,
                    description);
            return;
        }
        if (code == null || OAuth2ErrorCodes.INVALID_GRANT.equals(code)) {
            JsonResponseWriter.writeFailedResponse(
                    response,
                    jsonMapper,
                    HttpStatus.BAD_REQUEST,
                    DefaultErrorResultCode.BAD_REQUEST);
            return;
        }
        JsonResponseWriter.writeFailedResponse(
                response,
                jsonMapper,
                HttpStatus.BAD_REQUEST,
                DefaultErrorResultCode.BAD_REQUEST,
                code);
    }

    private String sanitizeDescription(String description) {
        String normalized = normalize(description);
        if (normalized == null) {
            return null;
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (OAuth2ErrorCodes.INVALID_GRANT.equals(lower)) {
            return null;
        }
        String prefix = "[" + OAuth2ErrorCodes.INVALID_GRANT + "]";
        if (lower.startsWith(prefix)) {
            String trimmed = normalize(normalized.substring(prefix.length()));
            return trimmed == null ? null : trimmed;
        }
        String colonPrefix = OAuth2ErrorCodes.INVALID_GRANT + ":";
        if (lower.startsWith(colonPrefix)) {
            String trimmed = normalize(normalized.substring(colonPrefix.length()));
            return trimmed == null ? null : trimmed;
        }
        if (lower.startsWith(OAuth2ErrorCodes.INVALID_GRANT)) {
            String trimmed =
                    normalize(normalized.substring(OAuth2ErrorCodes.INVALID_GRANT.length()));
            return trimmed == null ? null : trimmed;
        }
        return normalized;
    }

    private record ResolvedError(String code, String description) {}
}
