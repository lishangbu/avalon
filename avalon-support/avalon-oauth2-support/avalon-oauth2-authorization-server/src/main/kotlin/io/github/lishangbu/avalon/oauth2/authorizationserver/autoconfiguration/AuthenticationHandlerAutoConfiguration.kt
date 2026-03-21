package io.github.lishangbu.avalon.oauth2.authorizationserver.autoconfiguration

import io.github.lishangbu.avalon.oauth2.authorizationserver.login.InMemoryLoginFailureTracker
import io.github.lishangbu.avalon.oauth2.authorizationserver.login.LoginFailureTracker
import io.github.lishangbu.avalon.oauth2.authorizationserver.web.authentication.OAuth2AccessTokenApiResultResponseAuthenticationSuccessHandler
import io.github.lishangbu.avalon.oauth2.authorizationserver.web.authentication.OAuth2ErrorApiResultAuthenticationFailureHandler
import io.github.lishangbu.avalon.oauth2.common.log.AuthenticationLogRecorder
import io.github.lishangbu.avalon.oauth2.common.properties.Oauth2Properties
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import tools.jackson.databind.json.JsonMapper

@AutoConfiguration
class AuthenticationHandlerAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun loginFailureTracker(
        oauth2PropertiesProvider: ObjectProvider<Oauth2Properties>,
    ): LoginFailureTracker = InMemoryLoginFailureTracker(oauth2PropertiesProvider.ifAvailable)

    @Bean
    @ConditionalOnMissingBean
    fun accessTokenResponseAuthenticationSuccessHandler(
        logRecorderProvider: ObjectProvider<AuthenticationLogRecorder>,
        authorizationServiceProvider: ObjectProvider<OAuth2AuthorizationService>,
        oauth2PropertiesProvider: ObjectProvider<Oauth2Properties>,
        jsonMapper: JsonMapper,
    ): OAuth2AccessTokenApiResultResponseAuthenticationSuccessHandler =
        OAuth2AccessTokenApiResultResponseAuthenticationSuccessHandler(
            logRecorderProvider.getIfAvailable { AuthenticationLogRecorder.noop() },
            authorizationServiceProvider.ifAvailable,
            oauth2PropertiesProvider.ifAvailable,
            jsonMapper,
        )

    @Bean
    @ConditionalOnMissingBean
    fun oauth2ErrorApiResultAuthenticationFailureHandler(
        logRecorderProvider: ObjectProvider<AuthenticationLogRecorder>,
        oauth2PropertiesProvider: ObjectProvider<Oauth2Properties>,
        jsonMapper: JsonMapper,
    ): OAuth2ErrorApiResultAuthenticationFailureHandler =
        OAuth2ErrorApiResultAuthenticationFailureHandler(
            logRecorderProvider.getIfAvailable { AuthenticationLogRecorder.noop() },
            oauth2PropertiesProvider.ifAvailable,
            jsonMapper,
        )
}
