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
 * OAuth2 令牌生成器自动配置
 *
 * 组合访问令牌、刷新令牌和 JWT 生成器
 */
@AutoConfiguration
class OAuth2TokenGeneratorAutoConfiguration(
    /** JWT 编码器 */
    private val jwtEncoder: JwtEncoder,
) {
    /** 创建令牌生成器 */
    @Bean
    fun tokenGenerator(): OAuth2TokenGenerator<*> =
        DelegatingOAuth2TokenGenerator(
            ReferenceOAuth2AccessTokenGenerator(),
            OAuth2RefreshTokenGenerator(),
            JwtGenerator(jwtEncoder),
        )
}
