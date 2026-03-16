package io.github.lishangbu.avalon.oauth2.authorizationserver.autoconfiguration;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import io.github.lishangbu.avalon.oauth2.authorizationserver.introspection.DefaultOpaqueTokenIntrospector;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;

class TokenAutoConfigurationTest {

    @Test
    void jwtDecoderUsesJwkSource() {
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet());
        JwtDecoder decoder = new JwtDecoderAutoConfiguration().jwtDecoder(jwkSource);

        assertNotNull(decoder);
    }

    @Test
    void jwtEncoderUsesJwkSource() {
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet());
        JwtEncoder encoder = new JwtEncoderAutoConfiguration().jwtEncoder(jwkSource);

        assertNotNull(encoder);
    }

    @Test
    void tokenGeneratorBeanCreated() {
        JwtEncoder encoder = Mockito.mock(JwtEncoder.class);
        OAuth2TokenGeneratorAutoConfiguration configuration =
                new OAuth2TokenGeneratorAutoConfiguration(encoder);

        OAuth2TokenGenerator<?> generator = configuration.tokenGenerator();

        assertNotNull(generator);
    }

    @Test
    void opaqueTokenIntrospectorBeanCreated() {
        OAuth2AuthorizationService authorizationService =
                Mockito.mock(OAuth2AuthorizationService.class);
        UserDetailsService userDetailsService = Mockito.mock(UserDetailsService.class);

        OpaqueTokenIntrospector introspector =
                new OpaqueTokenIntrospectorAutoConfiguration()
                        .opaqueTokenIntrospector(authorizationService, userDetailsService);

        assertNotNull(introspector);
        assertNotNull(new DefaultOpaqueTokenIntrospector(authorizationService, userDetailsService));
    }
}
