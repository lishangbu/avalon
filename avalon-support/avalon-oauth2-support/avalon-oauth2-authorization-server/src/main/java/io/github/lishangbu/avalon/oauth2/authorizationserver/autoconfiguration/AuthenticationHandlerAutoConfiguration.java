package io.github.lishangbu.avalon.oauth2.authorizationserver.autoconfiguration;

import io.github.lishangbu.avalon.oauth2.authorizationserver.web.authentication.OAuth2AccessTokenApiResultResponseAuthenticationSuccessHandler;
import io.github.lishangbu.avalon.oauth2.authorizationserver.web.authentication.OAuth2ErrorApiResultAuthenticationFailureHandler;
import io.github.lishangbu.avalon.oauth2.authorizationserver.login.InMemoryLoginFailureTracker;
import io.github.lishangbu.avalon.oauth2.authorizationserver.login.LoginFailureTracker;
import io.github.lishangbu.avalon.oauth2.common.log.AuthenticationLogRecorder;
import io.github.lishangbu.avalon.oauth2.common.properties.Oauth2Properties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;

@AutoConfiguration
public class AuthenticationHandlerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public LoginFailureTracker loginFailureTracker(
            ObjectProvider<Oauth2Properties> oauth2PropertiesProvider) {
        return new InMemoryLoginFailureTracker(oauth2PropertiesProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean
    public OAuth2AccessTokenApiResultResponseAuthenticationSuccessHandler
            accessTokenResponseAuthenticationSuccessHandler(
                    ObjectProvider<AuthenticationLogRecorder> logRecorderProvider,
                    ObjectProvider<OAuth2AuthorizationService> authorizationServiceProvider,
                    ObjectProvider<Oauth2Properties> oauth2PropertiesProvider) {
        return new OAuth2AccessTokenApiResultResponseAuthenticationSuccessHandler(
                logRecorderProvider.getIfAvailable(AuthenticationLogRecorder::noop),
                authorizationServiceProvider.getIfAvailable(),
                oauth2PropertiesProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean
    public OAuth2ErrorApiResultAuthenticationFailureHandler
            oauth2ErrorApiResultAuthenticationFailureHandler(
                    ObjectProvider<AuthenticationLogRecorder> logRecorderProvider,
                    ObjectProvider<Oauth2Properties> oauth2PropertiesProvider) {
        return new OAuth2ErrorApiResultAuthenticationFailureHandler(
                logRecorderProvider.getIfAvailable(AuthenticationLogRecorder::noop),
                oauth2PropertiesProvider.getIfAvailable());
    }
}
