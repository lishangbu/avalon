package io.github.lishangbu.avalon.oauth2.authorizationserver.autoconfiguration

import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration
import org.springframework.security.oauth2.jwt.JwtDecoder

/**
 * JwtDecoder 自动装配 提供基于 JWKSource 的 JwtDecoder 实例，用于解码签名的访问令牌
 *
 * @param jwkSource JWKSource 实例
 * @return JwtDecoder
 * @author lishangbu
 * @since 2025/8/17 An instance of JwtDecoder for decoding signed access tokens.
 */
@AutoConfiguration
class JwtDecoderAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun jwtDecoder(jwkSource: JWKSource<SecurityContext>): JwtDecoder = OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource)
}
