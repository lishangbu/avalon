package io.github.lishangbu.avalon.oauth2.authorizationserver.autoconfiguration

import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration
import org.springframework.security.oauth2.jwt.JwtDecoder

/**
 * JWT 解码器自动配置
 *
 * 基于 [JWKSource] 创建默认的 [JwtDecoder]
 */
@AutoConfiguration
class JwtDecoderAutoConfiguration {
    /** 创建 JWT 解码器 */
    @Bean
    @ConditionalOnMissingBean
    fun jwtDecoder(jwkSource: JWKSource<SecurityContext>): JwtDecoder = OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource)
}
