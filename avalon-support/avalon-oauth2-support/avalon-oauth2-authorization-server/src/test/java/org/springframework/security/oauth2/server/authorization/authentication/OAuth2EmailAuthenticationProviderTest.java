package org.springframework.security.oauth2.server.authorization.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import io.github.lishangbu.avalon.oauth2.authorizationserver.authentication.EmailAuthenticationToken;
import io.github.lishangbu.avalon.oauth2.authorizationserver.login.LoginFailureTracker;
import io.github.lishangbu.avalon.oauth2.common.core.AuthorizationGrantTypeSupport;
import io.github.lishangbu.avalon.oauth2.common.userdetails.UserInfo;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

class OAuth2EmailAuthenticationProviderTest {

    @BeforeEach
    void setAuthorizationServerContext() {
        AuthorizationServerSettings settings =
                AuthorizationServerSettings.builder()
                        .issuer("https://example.org")
                        .build();
        AuthorizationServerContext context =
                new AuthorizationServerContext() {
                    @Override
                    public String getIssuer() {
                        return settings.getIssuer();
                    }

                    @Override
                    public AuthorizationServerSettings getAuthorizationServerSettings() {
                        return settings;
                    }
                };
        AuthorizationServerContextHolder.setContext(context);
    }

    @AfterEach
    void resetAuthorizationServerContext() {
        AuthorizationServerContextHolder.resetContext();
    }

    @Test
    void rejectsUnsupportedGrantType() {
        RegisteredClient registeredClient = registeredClient(false, false);
        OAuth2EmailAuthenticationProvider provider = provider(registeredClient, null, null, null);

        OAuth2EmailAuthorizationGrantAuthenticationToken token =
                emailGrantToken(registeredClient, Set.of());

        OAuth2AuthenticationException exception =
                assertThrows(OAuth2AuthenticationException.class, () -> provider.authenticate(token));
        assertEquals(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT, exception.getError().getErrorCode());
    }

    @Test
    void rejectsWhenAccountLockedSeconds() {
        RegisteredClient registeredClient = registeredClient(true, false);
        LoginFailureTracker tracker = lockedTracker(Duration.ofSeconds(30));
        OAuth2EmailAuthenticationProvider provider =
                provider(registeredClient, tracker, null, null);

        OAuth2AuthenticationException exception =
                assertThrows(
                        OAuth2AuthenticationException.class,
                        () -> provider.authenticate(emailGrantToken(registeredClient, Set.of())));

        assertEquals(OAuth2ErrorCodes.INVALID_GRANT, exception.getError().getErrorCode());
        assertTrue(exception.getError().getDescription().contains("30"));
    }

    @Test
    void rejectsWhenAccountLockedMinutes() {
        RegisteredClient registeredClient = registeredClient(true, false);
        LoginFailureTracker tracker = lockedTracker(Duration.ofSeconds(61));
        OAuth2EmailAuthenticationProvider provider =
                provider(registeredClient, tracker, null, null);

        OAuth2AuthenticationException exception =
                assertThrows(
                        OAuth2AuthenticationException.class,
                        () -> provider.authenticate(emailGrantToken(registeredClient, Set.of())));

        assertEquals(OAuth2ErrorCodes.INVALID_GRANT, exception.getError().getErrorCode());
        assertTrue(exception.getError().getDescription().contains("2"));
    }

    @Test
    void wrapsAuthenticationException() {
        RegisteredClient registeredClient = registeredClient(true, false);
        AuthenticationManager authenticationManager =
                authenticationManagerThrowing(new BadCredentialsException("bad"));
        LoginFailureTracker tracker = Mockito.mock(LoginFailureTracker.class);
        OAuth2TokenGenerator<OAuth2Token> tokenGenerator = new TestTokenGenerator();
        OAuth2AuthorizationService authorizationService = Mockito.mock(OAuth2AuthorizationService.class);

        OAuth2EmailAuthenticationProvider provider =
                new OAuth2EmailAuthenticationProvider(
                        authenticationManager,
                        authorizationService,
                        tokenGenerator,
                        tracker);

        OAuth2AuthenticationException exception =
                assertThrows(
                        OAuth2AuthenticationException.class,
                        () -> provider.authenticate(emailGrantToken(registeredClient, Set.of())));

        assertEquals(OAuth2ErrorCodes.INVALID_GRANT, exception.getError().getErrorCode());
        verify(tracker).onFailure("user@example.com");
    }

    @Test
    void rethrowsOAuth2AuthenticationException() {
        RegisteredClient registeredClient = registeredClient(true, false);
        OAuth2AuthenticationException oauth2Exception =
                new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_GRANT);
        AuthenticationManager authenticationManager = authenticationManagerThrowing(oauth2Exception);

        OAuth2EmailAuthenticationProvider provider =
                new OAuth2EmailAuthenticationProvider(
                        authenticationManager,
                        Mockito.mock(OAuth2AuthorizationService.class),
                        new TestTokenGenerator());

        OAuth2AuthenticationException exception =
                assertThrows(
                        OAuth2AuthenticationException.class,
                        () -> provider.authenticate(emailGrantToken(registeredClient, Set.of())));

        assertEquals(OAuth2ErrorCodes.INVALID_GRANT, exception.getError().getErrorCode());
    }

    @Test
    void rejectsInvalidScopes() {
        RegisteredClient registeredClient = registeredClient(true, false);
        AuthenticationManager authenticationManager = authenticationManagerSuccess();
        OAuth2AuthorizationService authorizationService = Mockito.mock(OAuth2AuthorizationService.class);
        TestTokenGenerator tokenGenerator = new TestTokenGenerator();
        tokenGenerator.put(OAuth2TokenType.ACCESS_TOKEN.getValue(), accessToken());

        OAuth2EmailAuthenticationProvider provider =
                new OAuth2EmailAuthenticationProvider(
                        authenticationManager, authorizationService, tokenGenerator);

        OAuth2AuthenticationException exception =
                assertThrows(
                        OAuth2AuthenticationException.class,
                        () -> provider.authenticate(emailGrantToken(registeredClient, Set.of("write"))));

        assertEquals(OAuth2ErrorCodes.INVALID_SCOPE, exception.getError().getErrorCode());
    }

    @Test
    void failsWhenAccessTokenGeneratorReturnsNull() {
        RegisteredClient registeredClient = registeredClient(true, false);
        AuthenticationManager authenticationManager = authenticationManagerSuccess();
        OAuth2AuthorizationService authorizationService = Mockito.mock(OAuth2AuthorizationService.class);
        TestTokenGenerator tokenGenerator = new TestTokenGenerator();

        OAuth2EmailAuthenticationProvider provider =
                new OAuth2EmailAuthenticationProvider(
                        authenticationManager, authorizationService, tokenGenerator);

        OAuth2AuthenticationException exception =
                assertThrows(
                        OAuth2AuthenticationException.class,
                        () -> provider.authenticate(emailGrantToken(registeredClient, Set.of())));

        assertEquals(OAuth2ErrorCodes.SERVER_ERROR, exception.getError().getErrorCode());
    }

    @Test
    void failsWhenRefreshTokenInvalidType() {
        RegisteredClient registeredClient = registeredClient(true, true);
        AuthenticationManager authenticationManager = authenticationManagerSuccess();
        OAuth2AuthorizationService authorizationService = Mockito.mock(OAuth2AuthorizationService.class);
        TestTokenGenerator tokenGenerator = new TestTokenGenerator();
        tokenGenerator.put(OAuth2TokenType.ACCESS_TOKEN.getValue(), accessToken());
        tokenGenerator.put(OAuth2TokenType.REFRESH_TOKEN.getValue(), accessToken());

        OAuth2EmailAuthenticationProvider provider =
                new OAuth2EmailAuthenticationProvider(
                        authenticationManager, authorizationService, tokenGenerator);

        OAuth2AuthenticationException exception =
                assertThrows(
                        OAuth2AuthenticationException.class,
                        () -> provider.authenticate(emailGrantToken(registeredClient, Set.of())));

        assertEquals(OAuth2ErrorCodes.SERVER_ERROR, exception.getError().getErrorCode());
    }

    @Test
    void failsWhenIdTokenInvalidType() {
        RegisteredClient registeredClient = registeredClient(true, false);
        AuthenticationManager authenticationManager = authenticationManagerSuccess();
        OAuth2AuthorizationService authorizationService = Mockito.mock(OAuth2AuthorizationService.class);
        TestTokenGenerator tokenGenerator = new TestTokenGenerator();
        tokenGenerator.put(OAuth2TokenType.ACCESS_TOKEN.getValue(), accessToken());
        tokenGenerator.put(OidcParameterNames.ID_TOKEN, accessToken());

        OAuth2EmailAuthenticationProvider provider =
                new OAuth2EmailAuthenticationProvider(
                        authenticationManager, authorizationService, tokenGenerator);

        OAuth2AuthenticationException exception =
                assertThrows(
                        OAuth2AuthenticationException.class,
                        () -> provider.authenticate(emailGrantToken(registeredClient, Set.of(OidcScopes.OPENID))));

        assertEquals(OAuth2ErrorCodes.SERVER_ERROR, exception.getError().getErrorCode());
    }

    @Test
    void authenticatesWithoutOpenIdScope() {
        RegisteredClient registeredClient = registeredClient(true, true);
        AuthenticationManager authenticationManager = authenticationManagerSuccess();
        OAuth2AuthorizationService authorizationService = Mockito.mock(OAuth2AuthorizationService.class);
        TestTokenGenerator tokenGenerator = new TestTokenGenerator();
        tokenGenerator.put(OAuth2TokenType.ACCESS_TOKEN.getValue(), accessToken());
        tokenGenerator.put(OAuth2TokenType.REFRESH_TOKEN.getValue(), refreshToken());

        LoginFailureTracker tracker = Mockito.mock(LoginFailureTracker.class);
        OAuth2EmailAuthenticationProvider provider =
                new OAuth2EmailAuthenticationProvider(
                        authenticationManager, authorizationService, tokenGenerator, tracker);

        OAuth2AccessTokenAuthenticationToken result =
                (OAuth2AccessTokenAuthenticationToken)
                        provider.authenticate(emailGrantToken(registeredClient, Set.of("read")));

        assertNotNull(result.getAccessToken());
        assertEquals(0, result.getAdditionalParameters().size());
        verify(authorizationService).save(Mockito.any());
        verify(tracker).onSuccess("user@example.com");
    }

    @Test
    void authenticatesWithOpenIdScope() {
        RegisteredClient registeredClient = registeredClient(true, true);
        AuthenticationManager authenticationManager = authenticationManagerSuccess();
        OAuth2AuthorizationService authorizationService = Mockito.mock(OAuth2AuthorizationService.class);
        TestTokenGenerator tokenGenerator = new TestTokenGenerator();
        tokenGenerator.put(OAuth2TokenType.ACCESS_TOKEN.getValue(), accessToken());
        tokenGenerator.put(OAuth2TokenType.REFRESH_TOKEN.getValue(), refreshToken());
        tokenGenerator.put(OidcParameterNames.ID_TOKEN, idToken());

        OAuth2EmailAuthenticationProvider provider =
                new OAuth2EmailAuthenticationProvider(
                        authenticationManager, authorizationService, tokenGenerator);

        OAuth2AccessTokenAuthenticationToken result =
                (OAuth2AccessTokenAuthenticationToken)
                        provider.authenticate(emailGrantToken(registeredClient, Set.of(OidcScopes.OPENID)));

        assertNotNull(result.getAdditionalParameters().get(OidcParameterNames.ID_TOKEN));
    }

    @Test
    void supportsEmailGrantToken() {
        OAuth2EmailAuthenticationProvider provider =
                new OAuth2EmailAuthenticationProvider(
                        authenticationManagerSuccess(),
                        Mockito.mock(OAuth2AuthorizationService.class),
                        new TestTokenGenerator());

        assertTrue(provider.supports(OAuth2EmailAuthorizationGrantAuthenticationToken.class));
        assertTrue(!provider.supports(EmailAuthenticationToken.class));
    }

    private static OAuth2EmailAuthenticationProvider provider(
            RegisteredClient registeredClient,
            LoginFailureTracker tracker,
            AuthenticationManager authenticationManager,
            OAuth2TokenGenerator<? extends OAuth2Token> generator) {
        AuthenticationManager manager =
                authenticationManager != null ? authenticationManager : authenticationManagerSuccess();
        OAuth2AuthorizationService authorizationService = Mockito.mock(OAuth2AuthorizationService.class);
        OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator =
                generator != null ? generator : new TestTokenGenerator();
        return new OAuth2EmailAuthenticationProvider(manager, authorizationService, tokenGenerator, tracker);
    }

    private static OAuth2EmailAuthorizationGrantAuthenticationToken emailGrantToken(
            RegisteredClient registeredClient, Set<String> scopes) {
        OAuth2ClientAuthenticationToken clientAuthenticationToken =
                new OAuth2ClientAuthenticationToken(
                        registeredClient,
                        ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
                        "secret");
        return new OAuth2EmailAuthorizationGrantAuthenticationToken(
                "user@example.com", "123456", clientAuthenticationToken, scopes, Map.of());
    }

    private static RegisteredClient registeredClient(boolean allowEmail, boolean allowRefresh) {
        RegisteredClient.Builder builder = RegisteredClient.withId("id")
                .clientId("client")
                .clientSecret("secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .tokenSettings(
                        TokenSettings.builder()
                                .accessTokenFormat(OAuth2TokenFormat.REFERENCE)
                                .accessTokenTimeToLive(Duration.ofMinutes(5))
                                .refreshTokenTimeToLive(Duration.ofMinutes(10))
                                .build())
                .scope("read")
                .scope(OidcScopes.OPENID);
        if (allowEmail) {
            builder.authorizationGrantType(AuthorizationGrantTypeSupport.EMAIL);
        } else {
            builder.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS);
        }
        if (allowRefresh) {
            builder.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN);
        }
        return builder.build();
    }

    private static AuthenticationManager authenticationManagerSuccess() {
        return authentication ->
                new EmailAuthenticationToken(
                        new UserInfo(
                                "user@example.com",
                                "123456",
                                Set.of(new SimpleGrantedAuthority("ROLE_USER"))),
                        "123456",
                        Set.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private static AuthenticationManager authenticationManagerThrowing(RuntimeException ex) {
        return authentication -> {
            throw ex;
        };
    }

    private static OAuth2AccessToken accessToken() {
        return new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "token",
                Instant.now(),
                Instant.now().plusSeconds(60),
                Set.of("read"));
    }

    private static OAuth2RefreshToken refreshToken() {
        return new OAuth2RefreshToken("refresh", Instant.now(), Instant.now().plusSeconds(120));
    }

    private static Jwt idToken() {
        return Jwt.withTokenValue("id")
                .header("alg", "none")
                .claim("sub", "user")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
    }

    private static LoginFailureTracker lockedTracker(Duration duration) {
        return new LoginFailureTracker() {
            @Override
            public boolean isEnabled() {
                return true;
            }

            @Override
            public Duration getRemainingLock(String username) {
                return duration;
            }

            @Override
            public void onFailure(String username) {}

            @Override
            public void onSuccess(String username) {}
        };
    }

    private static final class TestTokenGenerator implements OAuth2TokenGenerator<OAuth2Token> {
        private final Map<String, OAuth2Token> tokens = new HashMap<>();

        @Override
        public OAuth2Token generate(OAuth2TokenContext context) {
            OAuth2TokenType type = context.getTokenType();
            if (type == null) {
                return null;
            }
            return tokens.get(type.getValue());
        }

        private void put(String tokenType, OAuth2Token token) {
            tokens.put(tokenType, token);
        }
    }
}
