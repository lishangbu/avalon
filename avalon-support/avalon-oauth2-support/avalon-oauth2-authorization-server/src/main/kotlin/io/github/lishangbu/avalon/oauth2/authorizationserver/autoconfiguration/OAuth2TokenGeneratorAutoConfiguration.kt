package io.github.lishangbu.avalon.oauth2.authorizationserver.autoconfiguration

import io.github.lishangbu.avalon.oauth2.authorizationserver.token.OAuth2RefreshTokenGenerator
import io.github.lishangbu.avalon.oauth2.authorizationserver.token.ReferenceOAuth2AccessTokenGenerator
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator

/**
 * OAuth2 Token 生成器自动装配 提供 Delegating 的 OAuth2TokenGenerator，包含 reference token、refresh token 与 JWT
 *
 * @author lishangbu
 * @since 2025/8/22 reference的token生成器 reference的refreshToken生成器 // reference的token生成器 //
 *   reference的refreshToken生成器
 */
@AutoConfiguration
class OAuth2TokenGeneratorAutoConfiguration(
    private val jwtEncoder: JwtEncoder,
) {
    @Bean
    fun tokenGenerator(): OAuth2TokenGenerator<*> =
        DelegatingOAuth2TokenGenerator(
            ReferenceOAuth2AccessTokenGenerator(),
            OAuth2RefreshTokenGenerator(),
            JwtGenerator(jwtEncoder),
        )
}
