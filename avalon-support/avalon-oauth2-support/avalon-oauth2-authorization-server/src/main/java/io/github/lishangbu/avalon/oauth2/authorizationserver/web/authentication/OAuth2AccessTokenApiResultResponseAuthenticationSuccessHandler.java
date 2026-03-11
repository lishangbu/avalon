package io.github.lishangbu.avalon.oauth2.authorizationserver.web.authentication;

import io.github.lishangbu.avalon.oauth2.common.log.AuthenticationLogRecord;
import io.github.lishangbu.avalon.oauth2.common.log.AuthenticationLogRecorder;
import io.github.lishangbu.avalon.oauth2.common.properties.Oauth2Properties;
import io.github.lishangbu.avalon.web.util.JsonResponseWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.function.Consumer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationContext;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.util.CollectionUtils;

/// An implementation of an `AuthenticationSuccessHandler` used for handling an
/// `OAuth2AccessTokenAuthenticationToken` and returning the `OAuth2AccessTokenResponse` Access
// Token Response

///
/// @author Dmitriy Dubson
/// @author lishangbu
/// @see AuthenticationSuccessHandler
/// @see OAuth2AccessTokenResponseHttpMessageConverter
/// @since 2025/8/25
public class OAuth2AccessTokenApiResultResponseAuthenticationSuccessHandler
        implements AuthenticationSuccessHandler {

    private final Log logger = LogFactory.getLog(getClass());

    private Consumer<OAuth2AccessTokenAuthenticationContext> accessTokenResponseCustomizer;
    private final AuthenticationLogRecorder authenticationLogRecorder;
    private final OAuth2AuthorizationService authorizationService;
    private final Oauth2Properties oauth2Properties;

    public OAuth2AccessTokenApiResultResponseAuthenticationSuccessHandler() {
        this(AuthenticationLogRecorder.noop(), null, null);
    }

    public OAuth2AccessTokenApiResultResponseAuthenticationSuccessHandler(
            AuthenticationLogRecorder authenticationLogRecorder) {
        this(authenticationLogRecorder, null, null);
    }

    public OAuth2AccessTokenApiResultResponseAuthenticationSuccessHandler(
            AuthenticationLogRecorder authenticationLogRecorder,
            OAuth2AuthorizationService authorizationService) {
        this(authenticationLogRecorder, authorizationService, null);
    }

    public OAuth2AccessTokenApiResultResponseAuthenticationSuccessHandler(
            AuthenticationLogRecorder authenticationLogRecorder,
            OAuth2AuthorizationService authorizationService,
            Oauth2Properties oauth2Properties) {
        this.authenticationLogRecorder =
                authenticationLogRecorder == null
                        ? AuthenticationLogRecorder.noop()
                        : authenticationLogRecorder;
        this.authorizationService = authorizationService;
        this.oauth2Properties = oauth2Properties;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        if (!(authentication
                instanceof OAuth2AccessTokenAuthenticationToken accessTokenAuthentication)) {
            if (this.logger.isErrorEnabled()) {
                this.logger.error(
                        Authentication.class.getSimpleName()
                                + " must be of type "
                                + OAuth2AccessTokenAuthenticationToken.class.getName()
                                + " but was "
                                + authentication.getClass().getName());
            }
            OAuth2Error error =
                    new OAuth2Error(
                            OAuth2ErrorCodes.SERVER_ERROR,
                            "Unable to process the access token response.",
                            null);
            throw new OAuth2AuthenticationException(error);
        }

        OAuth2AccessToken accessToken = accessTokenAuthentication.getAccessToken();
        OAuth2RefreshToken refreshToken = accessTokenAuthentication.getRefreshToken();
        Map<String, Object> additionalParameters =
                accessTokenAuthentication.getAdditionalParameters();

        OAuth2AccessTokenResponse.Builder builder =
                OAuth2AccessTokenResponse.withToken(accessToken.getTokenValue())
                        .tokenType(accessToken.getTokenType())
                        .scopes(accessToken.getScopes());
        if (accessToken.getIssuedAt() != null && accessToken.getExpiresAt() != null) {
            builder.expiresIn(
                    ChronoUnit.SECONDS.between(
                            accessToken.getIssuedAt(), accessToken.getExpiresAt()));
        }
        if (refreshToken != null) {
            builder.refreshToken(refreshToken.getTokenValue());
        }
        if (!CollectionUtils.isEmpty(additionalParameters)) {
            builder.additionalParameters(additionalParameters);
        }

        if (this.accessTokenResponseCustomizer != null) {
            // @formatter:off
            OAuth2AccessTokenAuthenticationContext accessTokenAuthenticationContext =
                    OAuth2AccessTokenAuthenticationContext.with(accessTokenAuthentication)
                            .accessTokenResponse(builder)
                            .build();
            // @formatter:on
            this.accessTokenResponseCustomizer.accept(accessTokenAuthenticationContext);
            if (this.logger.isTraceEnabled()) {
                this.logger.trace("Customized access token response");
            }
        }

        OAuth2AccessTokenResponse accessTokenResponse = builder.build();
        recordAuthenticationSuccess(request, accessTokenAuthentication);
        JsonResponseWriter.writeSuccessResponse(response, accessTokenResponse);
    }

    private void recordAuthenticationSuccess(
            HttpServletRequest request,
            OAuth2AccessTokenAuthenticationToken accessTokenAuthentication) {
        try {
            Object principal = accessTokenAuthentication.getPrincipal();
            String grantType =
                    resolveGrantType(request, accessTokenAuthentication.getAdditionalParameters());
            String username =
                    resolveUsername(
                            principal, request, accessTokenAuthentication, grantType);
            String clientId = resolveClientId(accessTokenAuthentication, principal);
            AuthenticationLogRecord record =
                    new AuthenticationLogRecord(
                            normalize(username),
                            normalize(clientId),
                            normalize(grantType),
                            resolveClientIp(request),
                            normalize(request.getHeader("User-Agent")),
                            true,
                            null,
                            Instant.now());
            authenticationLogRecorder.record(record);
        } catch (Exception ex) {
            if (this.logger.isWarnEnabled()) {
                this.logger.warn("Failed to record authentication log", ex);
            }
        }
    }

    private String resolveUsername(
            Object principal,
            HttpServletRequest request,
            OAuth2AccessTokenAuthenticationToken accessTokenAuthentication,
            String grantType) {
        if (AuthorizationGrantType.CLIENT_CREDENTIALS.getValue().equals(grantType)) {
            return null;
        }
        String requestUsername = normalize(request.getParameter(resolveUsernameParameterName()));
        if (requestUsername != null) {
            return requestUsername;
        }
        String authorizationUsername = resolveAuthorizationUsername(accessTokenAuthentication);
        if (authorizationUsername != null) {
            return authorizationUsername;
        }
        if (principal == null) {
            return null;
        }
        if (principal instanceof OAuth2ClientAuthenticationToken) {
            return null;
        }
        if (principal instanceof Authentication authentication) {
            return normalize(authentication.getName());
        }
        if (principal instanceof Principal genericPrincipal) {
            return normalize(genericPrincipal.getName());
        }
        return normalize(principal.toString());
    }

    private String resolveAuthorizationUsername(
            OAuth2AccessTokenAuthenticationToken accessTokenAuthentication) {
        if (authorizationService == null || accessTokenAuthentication == null) {
            return null;
        }
        OAuth2AccessToken accessToken = accessTokenAuthentication.getAccessToken();
        if (accessToken == null) {
            return null;
        }
        OAuth2Authorization authorization =
                authorizationService.findByToken(
                        accessToken.getTokenValue(), OAuth2TokenType.ACCESS_TOKEN);
        if (authorization == null) {
            return null;
        }
        return normalize(authorization.getPrincipalName());
    }

    private String resolveClientId(
            OAuth2AccessTokenAuthenticationToken accessTokenAuthentication, Object principal) {
        if (accessTokenAuthentication.getRegisteredClient() != null) {
            return normalize(accessTokenAuthentication.getRegisteredClient().getClientId());
        }
        if (principal instanceof OAuth2ClientAuthenticationToken clientAuthenticationToken) {
            return normalize(clientAuthenticationToken.getName());
        }
        return null;
    }

    private String resolveGrantType(
            HttpServletRequest request, Map<String, Object> additionalParameters) {
        String grantType = normalize(request.getParameter("grant_type"));
        if (grantType != null) {
            return grantType;
        }
        if (additionalParameters != null) {
            Object value = additionalParameters.get("grant_type");
            if (value instanceof String text && !text.isBlank()) {
                return text;
            }
        }
        return null;
    }

    private String resolveUsernameParameterName() {
        if (oauth2Properties == null) {
            return "username";
        }
        String configured = oauth2Properties.getUsernameParameterName();
        return normalize(configured) == null ? "username" : configured;
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
}
