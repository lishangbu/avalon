package io.github.lishangbu.avalon.oauth2.authorizationserver.autoconfiguration

import io.github.lishangbu.avalon.oauth2.authorizationserver.introspection.DefaultOpaqueTokenIntrospector
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.context.annotation.Bean
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector

/**
 * 不透明令牌内省器自动配置
 *
 * 基于授权服务和用户服务创建默认的 [OpaqueTokenIntrospector]
 */
@AutoConfiguration
class OpaqueTokenIntrospectorAutoConfiguration {
    /** 创建不透明令牌内省器 */
    @Bean
    @ConditionalOnBean(value = [OAuth2AuthorizationService::class, UserDetailsService::class])
    fun opaqueTokenIntrospector(
        oAuth2AuthorizationService: OAuth2AuthorizationService,
        userDetailsService: UserDetailsService,
    ): OpaqueTokenIntrospector = DefaultOpaqueTokenIntrospector(oAuth2AuthorizationService, userDetailsService)
}
