package io.github.lishangbu.avalon.oauth2.authorizationserver.autoconfiguration

import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder

/**
 * JWT 编码器自动配置
 *
 * 基于 Nimbus 提供默认的 [JwtEncoder]
 */
@AutoConfiguration
class JwtEncoderAutoConfiguration {
    /** 创建 JWT 编码器 */
    @Bean
    @ConditionalOnMissingBean
    fun jwtEncoder(jwkSource: JWKSource<SecurityContext>): JwtEncoder = NimbusJwtEncoder(jwkSource)
}
