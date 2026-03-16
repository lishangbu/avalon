package io.github.lishangbu.avalon.oauth2.authorizationserver.autoconfiguration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lishangbu.avalon.oauth2.authorizationserver.login.InMemoryLoginFailureTracker;
import io.github.lishangbu.avalon.oauth2.authorizationserver.login.LoginFailureTracker;
import io.github.lishangbu.avalon.oauth2.authorizationserver.web.authentication.OAuth2AccessTokenApiResultResponseAuthenticationSuccessHandler;
import io.github.lishangbu.avalon.oauth2.authorizationserver.web.authentication.OAuth2ErrorApiResultAuthenticationFailureHandler;
import io.github.lishangbu.avalon.oauth2.common.properties.Oauth2Properties;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2EmailAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2PasswordAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2SmsAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;
import tools.jackson.databind.json.JsonMapper;

class AuthorizationServerAutoConfigurationTest {

    @Test
    void buildsSecurityFilterChainAndRegistersPasswordProvider() throws Exception {
        AuthorizationServerAutoConfiguration configuration =
                new AuthorizationServerAutoConfiguration();

        HttpSecurity httpSecurity = Mockito.mock(HttpSecurity.class, Answers.RETURNS_SELF);

        Mockito.when(httpSecurity.securityMatcher(Mockito.any(RequestMatcher.class)))
                .thenReturn(httpSecurity);
        Mockito.when(httpSecurity.csrf(Mockito.any())).thenReturn(httpSecurity);
        Mockito.when(httpSecurity.cors(Mockito.any())).thenReturn(httpSecurity);
        Mockito.when(httpSecurity.formLogin(Mockito.any())).thenReturn(httpSecurity);
        Mockito.when(httpSecurity.sessionManagement(Mockito.any())).thenReturn(httpSecurity);
        Mockito.when(httpSecurity.rememberMe(Mockito.any())).thenReturn(httpSecurity);

        ApplicationContext applicationContext = new GenericApplicationContext();
        ((GenericApplicationContext) applicationContext).refresh();

        Mockito.doAnswer(
                        invocation -> {
                            @SuppressWarnings("unchecked")
                            Customizer<
                                            AuthorizeHttpRequestsConfigurer<HttpSecurity>
                                                    .AuthorizationManagerRequestMatcherRegistry>
                                    customizer = invocation.getArgument(0, Customizer.class);
                            AuthorizeHttpRequestsConfigurer<HttpSecurity> configurer =
                                    new AuthorizeHttpRequestsConfigurer<>(applicationContext);
                            customizer.customize(configurer.getRegistry());
                            return httpSecurity;
                        })
                .when(httpSecurity)
                .authorizeHttpRequests(Mockito.any());

        Mockito.doAnswer(
                        invocation -> {
                            @SuppressWarnings("unchecked")
                            Customizer<OAuth2LoginConfigurer<HttpSecurity>> customizer =
                                    invocation.getArgument(0, Customizer.class);
                            customizer.customize(new OAuth2LoginConfigurer<>());
                            return httpSecurity;
                        })
                .when(httpSecurity)
                .oauth2Login(Mockito.any());

        Mockito.doAnswer(
                        invocation -> {
                            @SuppressWarnings("unchecked")
                            Customizer<ExceptionHandlingConfigurer<HttpSecurity>> customizer =
                                    invocation.getArgument(0, Customizer.class);
                            customizer.customize(new ExceptionHandlingConfigurer<>());
                            return httpSecurity;
                        })
                .when(httpSecurity)
                .exceptionHandling(Mockito.any());

        Mockito.doAnswer(
                        invocation -> {
                            OAuth2AuthorizationServerConfigurer configurer =
                                    invocation.getArgument(
                                            0, OAuth2AuthorizationServerConfigurer.class);
                            @SuppressWarnings("unchecked")
                            Customizer<OAuth2AuthorizationServerConfigurer> customizer =
                                    invocation.getArgument(1, Customizer.class);
                            customizer.customize(configurer);
                            return httpSecurity;
                        })
                .when(httpSecurity)
                .with(Mockito.any(OAuth2AuthorizationServerConfigurer.class), Mockito.any());

        DefaultSecurityFilterChain filterChain = Mockito.mock(DefaultSecurityFilterChain.class);
        Mockito.when(httpSecurity.build()).thenReturn(filterChain);

        AuthenticationManager authenticationManager = Mockito.mock(AuthenticationManager.class);
        OAuth2AuthorizationService authorizationService =
                Mockito.mock(OAuth2AuthorizationService.class);
        @SuppressWarnings("unchecked")
        OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator =
                Mockito.mock(OAuth2TokenGenerator.class);

        Mockito.when(httpSecurity.getSharedObject(AuthenticationManager.class))
                .thenReturn(authenticationManager);
        Mockito.when(httpSecurity.getSharedObject(OAuth2AuthorizationService.class))
                .thenReturn(authorizationService);
        Mockito.when(httpSecurity.getSharedObject(OAuth2TokenGenerator.class))
                .thenReturn(tokenGenerator);

        ArgumentCaptor<AuthenticationProvider> providerCaptor =
                ArgumentCaptor.forClass(AuthenticationProvider.class);
        Mockito.when(httpSecurity.authenticationProvider(providerCaptor.capture()))
                .thenReturn(httpSecurity);

        Oauth2Properties properties = new Oauth2Properties();
        LoginFailureTracker loginFailureTracker = new InMemoryLoginFailureTracker(properties);
        OAuth2AccessTokenApiResultResponseAuthenticationSuccessHandler successHandler =
                new OAuth2AccessTokenApiResultResponseAuthenticationSuccessHandler(
                        new JsonMapper());
        OAuth2ErrorApiResultAuthenticationFailureHandler failureHandler =
                new OAuth2ErrorApiResultAuthenticationFailureHandler(new JsonMapper());
        JsonMapper jsonMapper = new JsonMapper();

        SecurityFilterChain filterChainResult =
                configuration.authorizationServerSecurityFilterChain(
                        httpSecurity,
                        properties,
                        successHandler,
                        failureHandler,
                        loginFailureTracker,
                        jsonMapper);

        assertSame(filterChain, filterChainResult);
        assertNotNull(providerCaptor.getAllValues());
        assertTrue(
                providerCaptor.getAllValues().stream()
                        .anyMatch(p -> p instanceof OAuth2PasswordAuthenticationProvider));
        assertTrue(
                providerCaptor.getAllValues().stream()
                        .anyMatch(p -> p instanceof OAuth2SmsAuthenticationProvider));
        assertTrue(
                providerCaptor.getAllValues().stream()
                        .anyMatch(p -> p instanceof OAuth2EmailAuthenticationProvider));
    }
}
