package io.github.lishangbu.avalon.oauth2.authorizationserver.token;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lishangbu.avalon.oauth2.common.constant.SecurityConstants;
import io.github.lishangbu.avalon.oauth2.common.core.AuthorizationGrantTypeSupport;
import io.github.lishangbu.avalon.oauth2.common.userdetails.UserInfo;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;

class TokenSupportTest {

    @Test
    void addsThirdPartyClaimsToIdToken() {
        Map<String, Object> thirdPartyClaims =
                Map.of(
                        "custom", "value",
                        "conflict", "third",
                        IdTokenClaimNames.ISS, "issuer",
                        IdTokenClaimNames.SUB, "user");
        OidcIdToken idToken =
                new OidcIdToken(
                        "token",
                        Instant.now(),
                        Instant.now().plusSeconds(60),
                        thirdPartyClaims);
        DefaultOidcUser oidcUser =
                new DefaultOidcUser(
                        List.of(new SimpleGrantedAuthority("ROLE_USER")),
                        idToken,
                        "sub");
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(oidcUser, "n/a", oidcUser.getAuthorities());

        JwtClaimsSet.Builder claimsBuilder =
                JwtClaimsSet.builder().claim("conflict", "existing");
        JwtEncodingContext context =
                JwtEncodingContext.with(
                                JwsHeader.with(SignatureAlgorithm.RS256),
                                claimsBuilder)
                        .tokenType(new OAuth2TokenType(OidcParameterNames.ID_TOKEN))
                        .principal(authentication)
                        .authorizedScopes(Set.of())
                        .build();

        new JwtOAuth2TokenCustomizer().customize(context);

        Map<String, Object> claims = context.getClaims().build().getClaims();
        assertEquals("existing", claims.get("conflict"));
        assertEquals("value", claims.get("custom"));
        assertFalse(claims.containsKey(IdTokenClaimNames.ISS));
    }

    @Test
    void addsLoginTypeForOauth2User() {
        DefaultOAuth2User oauth2User =
                new DefaultOAuth2User(
                        List.of(new SimpleGrantedAuthority("ROLE_USER")),
                        Map.of(
                                SecurityConstants.LOGIN_TYPE, "sms",
                                IdTokenClaimNames.SUB, "user"),
                        "sub");
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(oauth2User, "n/a", oauth2User.getAuthorities());
        JwtEncodingContext context =
                JwtEncodingContext.with(
                                JwsHeader.with(SignatureAlgorithm.RS256),
                                JwtClaimsSet.builder())
                        .tokenType(new OAuth2TokenType("access"))
                        .principal(authentication)
                        .authorizedScopes(Set.of())
                        .build();

        new JwtOAuth2TokenCustomizer().customize(context);

        Map<String, Object> claims = context.getClaims().build().getClaims();
        assertEquals("sms", claims.get(SecurityConstants.LOGIN_TYPE));
    }

    @Test
    void addsAuthoritiesAndScopesForUserInfo() {
        UserInfo userInfo =
                new UserInfo(
                        "user",
                        "pwd",
                        List.of(new SimpleGrantedAuthority("ROLE_USER")));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userInfo, "pwd", userInfo.getAuthorities());
        JwtEncodingContext context =
                JwtEncodingContext.with(
                                JwsHeader.with(SignatureAlgorithm.RS256),
                                JwtClaimsSet.builder())
                        .tokenType(new OAuth2TokenType("access"))
                        .principal(authentication)
                        .authorizedScopes(Set.of("scope1"))
                        .build();

        new JwtOAuth2TokenCustomizer().customize(context);

        Object authorities =
                context.getClaims().build().getClaims().get(SecurityConstants.AUTHORITIES_KEY);
        assertNotNull(authorities);
        @SuppressWarnings("unchecked")
        Set<String> authoritySet = (Set<String>) authorities;
        assertTrue(authoritySet.contains("ROLE_USER"));
        assertTrue(authoritySet.contains("scope1"));
    }

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
        AuthorizationServerContext serverContext = new AuthorizationServerContext() {
            @Override
            public String getIssuer() {
                return "https://issuer";
            }

            @Override
            public AuthorizationServerSettings getAuthorizationServerSettings() {
                return AuthorizationServerSettings.builder().issuer("https://issuer").build();
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
