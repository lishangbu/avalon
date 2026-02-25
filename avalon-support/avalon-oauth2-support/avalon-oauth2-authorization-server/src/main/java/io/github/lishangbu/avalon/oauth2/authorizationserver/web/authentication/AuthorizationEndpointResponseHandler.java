package io.github.lishangbu.avalon.oauth2.authorizationserver.web.authentication;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.function.Consumer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationContext;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/// An implementation of an {@link AuthenticationSuccessHandler} used for handling an {@link
/// OAuth2AccessTokenAuthenticationToken} and returning the {@link OAuth2AccessTokenResponse Access
/// Token Response}
///
/// @author Dmitriy Dubson
/// @author lishangbu
/// @see AuthenticationSuccessHandler
/// @see OAuth2AccessTokenResponseHttpMessageConverter
public class AuthorizationEndpointResponseHandler implements AuthenticationSuccessHandler {

    private final Log logger = LogFactory.getLog(getClass());

    private final HttpMessageConverter<OAuth2AccessTokenResponse> accessTokenResponseConverter =
            new OAuth2AccessTokenResponseHttpMessageConverter();

    private Consumer<OAuth2AccessTokenAuthenticationContext> accessTokenResponseCustomizer;

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
        ServletServerHttpResponse httpResponse = new ServletServerHttpResponse(response);
        this.accessTokenResponseConverter.write(accessTokenResponse, null, httpResponse);
    }

    /// Sets the {@code Consumer} providing access to the {@link
    /// OAuth2AccessTokenAuthenticationContext} containing an {@link
    // OAuth2AccessTokenResponse.Builder}
    /// and additional context information.
    ///
    /// @param accessTokenResponseCustomizer the {@code Consumer} providing access to the {@link
    ///                                          OAuth2AccessTokenAuthenticationContext} containing
    // an
    // {@link

    ///                                          OAuth2AccessTokenResponse.Builder}
    public void setAccessTokenResponseCustomizer(
            Consumer<OAuth2AccessTokenAuthenticationContext> accessTokenResponseCustomizer) {
        Assert.notNull(
                accessTokenResponseCustomizer, "accessTokenResponseCustomizer cannot be null");
        this.accessTokenResponseCustomizer = accessTokenResponseCustomizer;
    }
}
