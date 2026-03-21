package io.github.lishangbu.avalon.oauth2.authorizationserver.autoconfiguration

import io.github.lishangbu.avalon.oauth2.authorizationserver.login.InMemoryLoginFailureTracker
import io.github.lishangbu.avalon.oauth2.authorizationserver.login.LoginFailureTracker
import io.github.lishangbu.avalon.oauth2.authorizationserver.web.authentication.OAuth2AccessTokenApiResultResponseAuthenticationSuccessHandler
import io.github.lishangbu.avalon.oauth2.authorizationserver.web.authentication.OAuth2ErrorApiResultAuthenticationFailureHandler
import io.github.lishangbu.avalon.oauth2.common.log.AuthenticationLogRecorder
import io.github.lishangbu.avalon.oauth2.common.properties.Oauth2Properties
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Answers
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.context.ApplicationContext
import org.springframework.context.support.GenericApplicationContext
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer
import org.springframework.security.oauth2.core.OAuth2Token
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2EmailAuthenticationProvider
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2PasswordAuthenticationProvider
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2SmsAuthenticationProvider
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator
import org.springframework.security.web.DefaultSecurityFilterChain
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.util.matcher.RequestMatcher
import tools.jackson.databind.json.JsonMapper

class AuthorizationServerAutoConfigurationTest {
    @Test
    fun buildsSecurityFilterChainAndRegistersPasswordProvider() {
        val configuration = AuthorizationServerAutoConfiguration()

        val httpSecurity = Mockito.mock(HttpSecurity::class.java, Answers.RETURNS_SELF)

        Mockito
            .`when`(
                httpSecurity.securityMatcher(ArgumentMatchers.any(RequestMatcher::class.java)),
            ).thenReturn(httpSecurity)
        Mockito.`when`(httpSecurity.csrf(ArgumentMatchers.any())).thenReturn(httpSecurity)
        Mockito.`when`(httpSecurity.cors(ArgumentMatchers.any())).thenReturn(httpSecurity)
        Mockito.`when`(httpSecurity.formLogin(ArgumentMatchers.any())).thenReturn(httpSecurity)
        Mockito
            .`when`(httpSecurity.sessionManagement(ArgumentMatchers.any()))
            .thenReturn(httpSecurity)
        Mockito.`when`(httpSecurity.rememberMe(ArgumentMatchers.any())).thenReturn(httpSecurity)

        val applicationContext: ApplicationContext =
            GenericApplicationContext().also { it.refresh() }

        Mockito
            .doAnswer { invocation ->
                @Suppress("UNCHECKED_CAST")
                val customizer =
                    invocation.getArgument(0)
                        as
                        Customizer<
                            AuthorizeHttpRequestsConfigurer<
                                HttpSecurity,
                                >.AuthorizationManagerRequestMatcherRegistry,
                            >
                val configurer = AuthorizeHttpRequestsConfigurer<HttpSecurity>(applicationContext)
                customizer.customize(configurer.registry)
                httpSecurity
            }.`when`(httpSecurity)
            .authorizeHttpRequests(ArgumentMatchers.any())

        Mockito
            .doAnswer { invocation ->
                @Suppress("UNCHECKED_CAST")
                val customizer =
                    invocation.getArgument(0) as Customizer<OAuth2LoginConfigurer<HttpSecurity>>
                customizer.customize(OAuth2LoginConfigurer())
                httpSecurity
            }.`when`(httpSecurity)
            .oauth2Login(ArgumentMatchers.any())

        Mockito
            .doAnswer { invocation ->
                @Suppress("UNCHECKED_CAST")
                val customizer =
                    invocation.getArgument(0)
                        as Customizer<ExceptionHandlingConfigurer<HttpSecurity>>
                customizer.customize(ExceptionHandlingConfigurer())
                httpSecurity
            }.`when`(httpSecurity)
            .exceptionHandling(ArgumentMatchers.any())

        Mockito
            .doAnswer { invocation ->
                val configurer =
                    invocation.getArgument(0, OAuth2AuthorizationServerConfigurer::class.java)

                @Suppress("UNCHECKED_CAST")
                val customizer =
                    invocation.getArgument(1) as Customizer<OAuth2AuthorizationServerConfigurer>
                customizer.customize(configurer)
                httpSecurity
            }.`when`(httpSecurity)
            .with(
                ArgumentMatchers.any(OAuth2AuthorizationServerConfigurer::class.java),
                ArgumentMatchers.any(),
            )

        val filterChain = Mockito.mock(DefaultSecurityFilterChain::class.java)
        Mockito.`when`(httpSecurity.build()).thenReturn(filterChain)

        val authenticationManager = Mockito.mock(AuthenticationManager::class.java)
        val authorizationService = Mockito.mock(OAuth2AuthorizationService::class.java)

        @Suppress("UNCHECKED_CAST")
        val tokenGenerator =
            Mockito.mock(OAuth2TokenGenerator::class.java) as OAuth2TokenGenerator<out OAuth2Token>

        Mockito
            .`when`(httpSecurity.getSharedObject(AuthenticationManager::class.java))
            .thenReturn(authenticationManager)
        Mockito
            .`when`(httpSecurity.getSharedObject(OAuth2AuthorizationService::class.java))
            .thenReturn(authorizationService)
        Mockito
            .`when`(httpSecurity.getSharedObject(OAuth2TokenGenerator::class.java))
            .thenReturn(tokenGenerator)

        val providerCaptor = ArgumentCaptor.forClass(AuthenticationProvider::class.java)
        Mockito
            .`when`(httpSecurity.authenticationProvider(providerCaptor.capture()))
            .thenReturn(httpSecurity)

        val properties = Oauth2Properties()
        val loginFailureTracker: LoginFailureTracker = InMemoryLoginFailureTracker(properties)
        val successHandler =
            OAuth2AccessTokenApiResultResponseAuthenticationSuccessHandler(
                AuthenticationLogRecorder.noop(),
                null,
                null,
                JsonMapper(),
            )
        val failureHandler =
            OAuth2ErrorApiResultAuthenticationFailureHandler(
                AuthenticationLogRecorder.noop(),
                null,
                JsonMapper(),
            )
        val jsonMapper = JsonMapper()

        val filterChainResult: SecurityFilterChain =
            configuration.authorizationServerSecurityFilterChain(
                httpSecurity,
                properties,
                successHandler,
                failureHandler,
                loginFailureTracker,
                jsonMapper,
            )

        assertSame(filterChain, filterChainResult)
        assertNotNull(providerCaptor.allValues)
        assertTrue(providerCaptor.allValues.any { it is OAuth2PasswordAuthenticationProvider })
        assertTrue(providerCaptor.allValues.any { it is OAuth2SmsAuthenticationProvider })
        assertTrue(providerCaptor.allValues.any { it is OAuth2EmailAuthenticationProvider })
    }
}
