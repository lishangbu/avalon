package org.springframework.security.oauth2.server.authorization.authentication;

import io.github.lishangbu.avalon.oauth2.authorizationserver.authentication.EmailAuthenticationToken;
import io.github.lishangbu.avalon.oauth2.authorizationserver.login.LoginFailureTracker;
import io.github.lishangbu.avalon.oauth2.common.core.AuthorizationGrantTypeSupport;
import java.security.Principal;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.log.LogMessage;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/// OAuth2 邮箱授权模式的认证提供者
///
/// 负责处理邮箱验证码授权类型的令牌申请
/// 校验客户端授权类型、邮箱验证码，以及生成并保存授权信息
///
/// @author lishangbu
/// @since 2026/3/13
public final class OAuth2EmailAuthenticationProvider implements AuthenticationProvider {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String ERROR_URI =
            "https://datatracker.ietf.org/doc/html/rfc6749#section-5.2";

    private static final OAuth2TokenType ID_TOKEN_TOKEN_TYPE =
            new OAuth2TokenType(OidcParameterNames.ID_TOKEN);

    private final AuthenticationManager authenticationManager;

    private final OAuth2AuthorizationService authorizationService;

    private final OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator;
    private final LoginFailureTracker loginFailureTracker;

    public OAuth2EmailAuthenticationProvider(
            AuthenticationManager authenticationManager,
            OAuth2AuthorizationService authorizationService,
            OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator) {
        this(authenticationManager, authorizationService, tokenGenerator, null);
    }

    public OAuth2EmailAuthenticationProvider(
            AuthenticationManager authenticationManager,
            OAuth2AuthorizationService authorizationService,
            OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator,
            LoginFailureTracker loginFailureTracker) {
        Assert.notNull(authorizationService, "authorizationService cannot be null");
        Assert.notNull(tokenGenerator, "tokenGenerator cannot be null");
        this.authenticationManager = authenticationManager;
        this.authorizationService = authorizationService;
        this.tokenGenerator = tokenGenerator;
        this.loginFailureTracker = loginFailureTracker;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        OAuth2EmailAuthorizationGrantAuthenticationToken emailGrantAuthenticationToken =
                (OAuth2EmailAuthorizationGrantAuthenticationToken) authentication;

        OAuth2ClientAuthenticationToken clientAuthenticationToken =
                (OAuth2ClientAuthenticationToken) emailGrantAuthenticationToken.getPrincipal();

        RegisteredClient registeredClient = clientAuthenticationToken.getRegisteredClient();

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Retrieved registered client");
        }

        if (!registeredClient.getAuthorizationGrantTypes().contains(AuthorizationGrantTypeSupport.EMAIL)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                        LogMessage.format(
                                "Invalid request: requested grant_type is not allowed"
                                        + " for registered client '%s'",
                                registeredClient.getId()));
            }
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT);
        }

        String email = emailGrantAuthenticationToken.getEmail();
        String emailCode = emailGrantAuthenticationToken.getEmailCode();

        if (loginFailureTracker != null && loginFailureTracker.isEnabled()) {
            Duration remainingLock = loginFailureTracker.getRemainingLock(email);
            if (remainingLock != null) {
                throw new OAuth2AuthenticationException(
                        new OAuth2Error(
                                OAuth2ErrorCodes.INVALID_GRANT,
                                buildLockMessage(remainingLock),
                                ERROR_URI));
            }
        }

        EmailAuthenticationToken emailAuthenticationToken =
                new EmailAuthenticationToken(email, emailCode);

        Authentication emailAuthentication;
        try {
            emailAuthentication = authenticationManager.authenticate(emailAuthenticationToken);
        } catch (AuthenticationException ex) {
            if (loginFailureTracker != null) {
                loginFailureTracker.onFailure(email);
            }
            if (ex instanceof OAuth2AuthenticationException oauth2Exception) {
                throw oauth2Exception;
            }
            OAuth2Error error =
                    new OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT, ex.getMessage(), ERROR_URI);
            throw new OAuth2AuthenticationException(error, ex);
        }

        if (loginFailureTracker != null) {
            loginFailureTracker.onSuccess(email);
        }

        Set<String> authorizedScopes = registeredClient.getScopes(); // Default to configured scopes
        Set<String> requestedScopes = emailGrantAuthenticationToken.getScopes();
        if (!CollectionUtils.isEmpty(requestedScopes)) {
            Set<String> unauthorizedScopes =
                    requestedScopes.stream()
                            .filter(
                                    requestedScope ->
                                            !registeredClient.getScopes().contains(requestedScope))
                            .collect(Collectors.toSet());
            if (!CollectionUtils.isEmpty(unauthorizedScopes)) {
                throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_SCOPE);
            }

            authorizedScopes = new LinkedHashSet<>(requestedScopes);
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Validated token request parameters");
        }

        DefaultOAuth2TokenContext.Builder tokenContextBuilder =
                DefaultOAuth2TokenContext.builder()
                        .registeredClient(registeredClient)
                        .principal(emailAuthentication)
                        .authorizationServerContext(AuthorizationServerContextHolder.getContext())
                        .authorizedScopes(authorizedScopes)
                        .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                        .authorizationGrantType(AuthorizationGrantTypeSupport.EMAIL)
                        .authorizationGrant(emailGrantAuthenticationToken);

        // ----- Access token -----
        OAuth2TokenContext tokenContext =
                tokenContextBuilder.tokenType(OAuth2TokenType.ACCESS_TOKEN).build();

        OAuth2Token generatedAccessToken = this.tokenGenerator.generate(tokenContext);

        if (generatedAccessToken == null) {
            OAuth2Error error =
                    new OAuth2Error(
                            OAuth2ErrorCodes.SERVER_ERROR,
                            "The token generator failed to generate the access token.",
                            ERROR_URI);
            throw new OAuth2AuthenticationException(error);
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Generated access token");
        }

        OAuth2Authorization.Builder authorizationBuilder =
                OAuth2Authorization.withRegisteredClient(registeredClient)
                        .principalName(email)
                        .authorizationGrantType(AuthorizationGrantTypeSupport.EMAIL)
                        .authorizedScopes(authorizedScopes)
                        .attribute(Principal.class.getName(), emailAuthentication);

        OAuth2AccessToken accessToken =
                OAuth2AuthenticationProviderUtils.accessToken(
                        authorizationBuilder, generatedAccessToken, tokenContext);

        // ----- Refresh token -----
        OAuth2RefreshToken refreshToken = null;
        // Do not issue refresh token to public client
        if (registeredClient.getAuthorizationGrantTypes().contains(AuthorizationGrantType.REFRESH_TOKEN)) {
            tokenContext = tokenContextBuilder.tokenType(OAuth2TokenType.REFRESH_TOKEN).build();
            OAuth2Token generatedRefreshToken = this.tokenGenerator.generate(tokenContext);
            if (generatedRefreshToken != null) {
                if (!(generatedRefreshToken instanceof OAuth2RefreshToken)) {
                    OAuth2Error error =
                            new OAuth2Error(
                                    OAuth2ErrorCodes.SERVER_ERROR,
                                    "The token generator failed to generate a valid refresh token.",
                                    ERROR_URI);
                    throw new OAuth2AuthenticationException(error);
                }

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Generated refresh token");
                }

                refreshToken = (OAuth2RefreshToken) generatedRefreshToken;
                authorizationBuilder.refreshToken(refreshToken);
            }
        }

        // ----- ID token -----
        OidcIdToken idToken;
        if (authorizedScopes.contains(OidcScopes.OPENID)) {
            tokenContext =
                    tokenContextBuilder
                            .tokenType(ID_TOKEN_TOKEN_TYPE)
                            // ID token customizer may need access to the access token and/or
                            // refresh token
                            .authorization(authorizationBuilder.build())
                            .build();
            OAuth2Token generatedIdToken = this.tokenGenerator.generate(tokenContext);
            if (!(generatedIdToken instanceof Jwt)) {
                OAuth2Error error =
                        new OAuth2Error(
                                OAuth2ErrorCodes.SERVER_ERROR,
                                "The token generator failed to generate the ID token.",
                                ERROR_URI);
                throw new OAuth2AuthenticationException(error);
            }

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Generated id token");
            }

            idToken =
                    new OidcIdToken(
                            generatedIdToken.getTokenValue(),
                            generatedIdToken.getIssuedAt(),
                            generatedIdToken.getExpiresAt(),
                            ((Jwt) generatedIdToken).getClaims());
            authorizationBuilder.token(
                    idToken,
                    (metadata) ->
                            metadata.put(
                                    OAuth2Authorization.Token.CLAIMS_METADATA_NAME,
                                    idToken.getClaims()));
        } else {
            idToken = null;
        }
        OAuth2Authorization authorization = authorizationBuilder.build();

        this.authorizationService.save(authorization);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Saved authorization");
        }

        Map<String, Object> additionalParameters = Collections.emptyMap();
        if (idToken != null) {
            additionalParameters = new HashMap<>();
            additionalParameters.put(OidcParameterNames.ID_TOKEN, idToken.getTokenValue());
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Authenticated token request");
        }

        OAuth2AccessTokenAuthenticationToken accessTokenAuthenticationResult =
                new OAuth2AccessTokenAuthenticationToken(
                        registeredClient,
                        clientAuthenticationToken,
                        accessToken,
                        refreshToken,
                        additionalParameters);
        accessTokenAuthenticationResult.setDetails(emailGrantAuthenticationToken.getDetails());
        return accessTokenAuthenticationResult;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return OAuth2EmailAuthorizationGrantAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private String buildLockMessage(Duration remainingLock) {
        long seconds = Math.max(1L, remainingLock == null ? 0L : remainingLock.getSeconds());
        if (seconds >= 60) {
            long minutes = (seconds + 59) / 60;
            return "账号已被锁定，请在" + minutes + "分钟后重试";
        }
        return "账号已被锁定，请在" + seconds + "秒后重试";
    }
}
