package io.github.lishangbu.avalon.oauth2.authorizationserver.token;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lishangbu.avalon.oauth2.common.core.AuthorizationGrantTypeSupport;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;

class TokenSupportTest {

    @Test
    void refreshTokenGeneratorReturnsNullForOtherTokenTypes() {
        OAuth2RefreshTokenGenerator generator = new OAuth2RefreshTokenGenerator();
        RegisteredClient client = registeredClient(OAuth2TokenFormat.REFERENCE);
        DefaultOAuth2TokenContext context =
                DefaultOAuth2TokenContext.builder()
                        .registeredClient(client)
                        .principal(new UsernamePasswordAuthenticationToken("user", "pwd"))
                        .authorizationGrantType(AuthorizationGrantTypeSupport.PASSWORD)
                        .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                        .build();

        assertNull(generator.generate(context));
    }

    @Test
    void refreshTokenGeneratorCreatesToken() {
        OAuth2RefreshTokenGenerator generator = new OAuth2RefreshTokenGenerator();
        RegisteredClient client = registeredClient(OAuth2TokenFormat.REFERENCE);
        DefaultOAuth2TokenContext context =
                DefaultOAuth2TokenContext.builder()
                        .registeredClient(client)
                        .principal(new UsernamePasswordAuthenticationToken("user", "pwd"))
                        .authorizationGrantType(AuthorizationGrantTypeSupport.PASSWORD)
                        .tokenType(OAuth2TokenType.REFRESH_TOKEN)
                        .build();

        OAuth2RefreshToken token = generator.generate(context);

        assertNotNull(token);
        assertTrue(token.getExpiresAt().isAfter(token.getIssuedAt()));
    }

    @Test
    void referenceAccessTokenGeneratorRespectsFormat() {
        ReferenceOAuth2AccessTokenGenerator generator = new ReferenceOAuth2AccessTokenGenerator();
        RegisteredClient client = registeredClient(OAuth2TokenFormat.SELF_CONTAINED);
        DefaultOAuth2TokenContext context =
                DefaultOAuth2TokenContext.builder()
                        .registeredClient(client)
                        .principal(new UsernamePasswordAuthenticationToken("user", "pwd"))
                        .authorizationGrantType(AuthorizationGrantTypeSupport.PASSWORD)
                        .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                        .build();

        assertNull(generator.generate(context));
    }

    @Test
    void referenceAccessTokenGeneratorCreatesTokenWithClaims() {
        ReferenceOAuth2AccessTokenGenerator generator = new ReferenceOAuth2AccessTokenGenerator();
        RegisteredClient client = registeredClient(OAuth2TokenFormat.REFERENCE);
        AuthorizationServerContext serverContext =
                new AuthorizationServerContext() {
                    @Override
                    public String getIssuer() {
                        return "https://issuer";
                    }

                    @Override
                    public AuthorizationServerSettings getAuthorizationServerSettings() {
                        return AuthorizationServerSettings.builder()
                                .issuer("https://issuer")
                                .build();
                    }
                };

        DefaultOAuth2TokenContext context =
                DefaultOAuth2TokenContext.builder()
                        .registeredClient(client)
                        .authorizationServerContext(serverContext)
                        .principal(new UsernamePasswordAuthenticationToken("user", "pwd"))
                        .authorizedScopes(Set.of("read"))
                        .authorizationGrantType(AuthorizationGrantTypeSupport.PASSWORD)
                        .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                        .build();

        OAuth2AccessToken token = generator.generate(context);

        assertNotNull(token);
        assertTrue(token instanceof org.springframework.security.oauth2.core.ClaimAccessor);
        Map<String, Object> claims =
                ((org.springframework.security.oauth2.core.ClaimAccessor) token).getClaims();
        assertEquals("https://issuer", claims.get("iss").toString());
        assertEquals("user", claims.get("sub"));
        assertTrue(claims.containsKey(OAuth2ParameterNames.SCOPE));
    }

    @Test
    void referenceAccessTokenGeneratorOmitsScopeWhenEmpty() {
        ReferenceOAuth2AccessTokenGenerator generator = new ReferenceOAuth2AccessTokenGenerator();
        RegisteredClient client = registeredClient(OAuth2TokenFormat.REFERENCE);
        DefaultOAuth2TokenContext context =
                DefaultOAuth2TokenContext.builder()
                        .registeredClient(client)
                        .principal(new UsernamePasswordAuthenticationToken("user", "pwd"))
                        .authorizedScopes(Set.of())
                        .authorizationGrantType(AuthorizationGrantTypeSupport.PASSWORD)
                        .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                        .build();

        OAuth2AccessToken token = generator.generate(context);

        assertNotNull(token);
        Map<String, Object> claims =
                ((org.springframework.security.oauth2.core.ClaimAccessor) token).getClaims();
        assertFalse(claims.containsKey(OAuth2ParameterNames.SCOPE));
    }

    private static RegisteredClient registeredClient(OAuth2TokenFormat format) {
        return RegisteredClient.withId("id")
                .clientId("client")
                .clientSecret("secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantTypeSupport.PASSWORD)
                .scope("read")
                .tokenSettings(
                        TokenSettings.builder()
                                .accessTokenFormat(format)
                                .accessTokenTimeToLive(Duration.ofMinutes(5))
                                .refreshTokenTimeToLive(Duration.ofMinutes(10))
                                .build())
                .build();
    }
}
