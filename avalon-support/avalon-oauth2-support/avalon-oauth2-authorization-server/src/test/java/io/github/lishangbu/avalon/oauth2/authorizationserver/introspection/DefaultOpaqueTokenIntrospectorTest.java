package io.github.lishangbu.avalon.oauth2.authorizationserver.introspection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.lishangbu.avalon.oauth2.common.core.AuthorizationGrantTypeSupport;
import io.github.lishangbu.avalon.oauth2.common.userdetails.UserInfo;
import java.security.Principal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

class DefaultOpaqueTokenIntrospectorTest {

    @Test
    void throwsWhenAuthorizationMissing() {
        OAuth2AuthorizationService authorizationService = Mockito.mock(OAuth2AuthorizationService.class);
        UserDetailsService userDetailsService = Mockito.mock(UserDetailsService.class);
        DefaultOpaqueTokenIntrospector introspector =
                new DefaultOpaqueTokenIntrospector(authorizationService, userDetailsService);

        assertThrows(
                InvalidBearerTokenException.class,
                () -> introspector.introspect("missing"));
    }

    @Test
    void returnsPrincipalForClientCredentials() {
        OAuth2Authorization authorization =
                authorizationWithGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS, "client", Map.of("key", "val"));
        OAuth2AuthorizationService authorizationService = Mockito.mock(OAuth2AuthorizationService.class);
        Mockito.when(
                        authorizationService.findByToken(
                                "token", OAuth2TokenType.ACCESS_TOKEN))
                .thenReturn(authorization);
        UserDetailsService userDetailsService = Mockito.mock(UserDetailsService.class);
        DefaultOpaqueTokenIntrospector introspector =
                new DefaultOpaqueTokenIntrospector(authorizationService, userDetailsService);

        OAuth2AuthenticatedPrincipal principal = introspector.introspect("token");

        assertNotNull(principal);
        assertEquals("client", principal.getName());
        assertEquals("val", principal.getAttribute("key"));
        assertEquals(DefaultOAuth2AuthenticatedPrincipal.class, principal.getClass());
    }

    @Test
    void returnsUserInfoWhenAvailable() {
        UserInfo userInfo =
                new UserInfo(
                        "user",
                        "pwd",
                        Set.of(new SimpleGrantedAuthority("ROLE_USER")));
        UsernamePasswordAuthenticationToken principal =
                new UsernamePasswordAuthenticationToken(userInfo, "pwd", userInfo.getAuthorities());
        OAuth2Authorization authorization =
                authorizationWithGrantType(AuthorizationGrantTypeSupport.PASSWORD, "user", Map.of("scope", "read"));
        authorization = OAuth2Authorization.from(authorization)
                .attribute(Principal.class.getName(), principal)
                .build();

        OAuth2AuthorizationService authorizationService = Mockito.mock(OAuth2AuthorizationService.class);
        Mockito.when(
                        authorizationService.findByToken(
                                "token", OAuth2TokenType.ACCESS_TOKEN))
                .thenReturn(authorization);
        UserDetailsService userDetailsService = Mockito.mock(UserDetailsService.class);
        Mockito.when(userDetailsService.loadUserByUsername("user")).thenReturn(userInfo);

        DefaultOpaqueTokenIntrospector introspector =
                new DefaultOpaqueTokenIntrospector(authorizationService, userDetailsService);

        OAuth2AuthenticatedPrincipal principalResult = introspector.introspect("token");

        assertSame(userInfo, principalResult);
        assertEquals("read", userInfo.getAttributes().get("scope"));
    }

    @Test
    void returnsNullWhenUserDetailsIsNotUserInfo() {
        User user = new User("user", "pwd", Set.of(new SimpleGrantedAuthority("ROLE_USER")));
        UsernamePasswordAuthenticationToken principal =
                new UsernamePasswordAuthenticationToken(user, "pwd", user.getAuthorities());
        OAuth2Authorization authorization =
                authorizationWithGrantType(AuthorizationGrantTypeSupport.PASSWORD, "user", Map.of());
        authorization = OAuth2Authorization.from(authorization)
                .attribute(Principal.class.getName(), principal)
                .build();

        OAuth2AuthorizationService authorizationService = Mockito.mock(OAuth2AuthorizationService.class);
        Mockito.when(
                        authorizationService.findByToken(
                                "token", OAuth2TokenType.ACCESS_TOKEN))
                .thenReturn(authorization);
        UserDetailsService userDetailsService = Mockito.mock(UserDetailsService.class);
        Mockito.when(userDetailsService.loadUserByUsername("user")).thenReturn(user);

        DefaultOpaqueTokenIntrospector introspector =
                new DefaultOpaqueTokenIntrospector(authorizationService, userDetailsService);

        assertNull(introspector.introspect("token"));
    }

    @Test
    void rethrowsUsernameNotFound() {
        User user = new User("user", "pwd", Set.of(new SimpleGrantedAuthority("ROLE_USER")));
        UsernamePasswordAuthenticationToken principal =
                new UsernamePasswordAuthenticationToken(user, "pwd", user.getAuthorities());
        OAuth2Authorization authorization =
                authorizationWithGrantType(AuthorizationGrantTypeSupport.PASSWORD, "user", Map.of());
        authorization = OAuth2Authorization.from(authorization)
                .attribute(Principal.class.getName(), principal)
                .build();
        OAuth2AuthorizationService authorizationService = Mockito.mock(OAuth2AuthorizationService.class);
        Mockito.when(
                        authorizationService.findByToken(
                                "token", OAuth2TokenType.ACCESS_TOKEN))
                .thenReturn(authorization);
        UserDetailsService userDetailsService = Mockito.mock(UserDetailsService.class);
        Mockito.when(userDetailsService.loadUserByUsername("user"))
                .thenThrow(new UsernameNotFoundException("missing"));

        DefaultOpaqueTokenIntrospector introspector =
                new DefaultOpaqueTokenIntrospector(authorizationService, userDetailsService);

        assertThrows(UsernameNotFoundException.class, () -> introspector.introspect("token"));
    }

    @Test
    void returnsNullOnUnexpectedException() {
        OAuth2Authorization authorization =
                authorizationWithGrantType(AuthorizationGrantTypeSupport.PASSWORD, "user", Map.of());
        OAuth2AuthorizationService authorizationService = Mockito.mock(OAuth2AuthorizationService.class);
        Mockito.when(
                        authorizationService.findByToken(
                                "token", OAuth2TokenType.ACCESS_TOKEN))
                .thenReturn(authorization);
        UserDetailsService userDetailsService = Mockito.mock(UserDetailsService.class);
        Mockito.when(userDetailsService.loadUserByUsername("user"))
                .thenThrow(new IllegalStateException("boom"));

        DefaultOpaqueTokenIntrospector introspector =
                new DefaultOpaqueTokenIntrospector(authorizationService, userDetailsService);

        assertNull(introspector.introspect("token"));
    }

    private static OAuth2Authorization authorizationWithGrantType(
            AuthorizationGrantType grantType, String principalName, Map<String, Object> claims) {
        RegisteredClient registeredClient = RegisteredClient.withId("id")
                .clientId("client")
                .clientSecret("secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(grantType)
                .scope("read")
                .tokenSettings(TokenSettings.builder().build())
                .build();
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "token",
                Instant.now(),
                Instant.now().plusSeconds(60),
                Set.of("read"));
        return OAuth2Authorization.withRegisteredClient(registeredClient)
                .principalName(principalName)
                .authorizationGrantType(grantType)
                .token(accessToken, metadata -> metadata.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME, claims))
                .build();
    }
}
