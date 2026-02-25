package io.github.lishangbu.avalon.oauth2.authorizationserver.autoconfiguration;

import io.github.lishangbu.avalon.oauth2.authorizationserver.introspection.DefaultOpaqueTokenIntrospector;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;

/// OpaqueTokenIntrospector 自动装配
///
/// 提供基于 OAuth2AuthorizationService 和 UserDetailsService 的 OpaqueTokenIntrospector Bean
///
/// @author lishangbu
/// @since 2025/8/22
@AutoConfiguration
public class OpaqueTokenIntrospectorAutoConfiguration {
    @Bean
    @ConditionalOnBean(value = {OAuth2AuthorizationService.class, UserDetailsService.class})
    public OpaqueTokenIntrospector opaqueTokenIntrospector(
            OAuth2AuthorizationService oAuth2AuthorizationService,
            UserDetailsService userDetailsService) {
        return new DefaultOpaqueTokenIntrospector(oAuth2AuthorizationService, userDetailsService);
    }
}
