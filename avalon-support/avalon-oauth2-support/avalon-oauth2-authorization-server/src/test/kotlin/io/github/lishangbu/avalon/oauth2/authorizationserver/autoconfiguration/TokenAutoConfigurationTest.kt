package io.github.lishangbu.avalon.oauth2.authorizationserver.autoconfiguration

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import io.github.lishangbu.avalon.oauth2.authorizationserver.introspection.DefaultOpaqueTokenIntrospector
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector

class TokenAutoConfigurationTest {
    @Test
    fun jwtDecoderUsesJwkSource() {
        val jwkSource: JWKSource<SecurityContext> = ImmutableJWKSet(JWKSet())
        val decoder: JwtDecoder = JwtDecoderAutoConfiguration().jwtDecoder(jwkSource)

        assertNotNull(decoder)
    }

    @Test
    fun jwtEncoderUsesJwkSource() {
        val jwkSource: JWKSource<SecurityContext> = ImmutableJWKSet(JWKSet())
        val encoder: JwtEncoder = JwtEncoderAutoConfiguration().jwtEncoder(jwkSource)

        assertNotNull(encoder)
    }

    @Test
    fun tokenGeneratorBeanCreated() {
        val encoder = Mockito.mock(JwtEncoder::class.java)
        val configuration = OAuth2TokenGeneratorAutoConfiguration(encoder)

        val generator: OAuth2TokenGenerator<*> = configuration.tokenGenerator()

        assertNotNull(generator)
    }

    @Test
    fun opaqueTokenIntrospectorBeanCreated() {
        val authorizationService = Mockito.mock(OAuth2AuthorizationService::class.java)
        val userDetailsService = Mockito.mock(UserDetailsService::class.java)

        val introspector: OpaqueTokenIntrospector =
            OpaqueTokenIntrospectorAutoConfiguration()
                .opaqueTokenIntrospector(authorizationService, userDetailsService)

        assertNotNull(introspector)
        assertNotNull(DefaultOpaqueTokenIntrospector(authorizationService, userDetailsService))
    }
}
