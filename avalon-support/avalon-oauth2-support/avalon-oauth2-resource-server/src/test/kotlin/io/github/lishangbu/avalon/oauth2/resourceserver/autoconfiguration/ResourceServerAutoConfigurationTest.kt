package io.github.lishangbu.avalon.oauth2.resourceserver.autoconfiguration

import io.github.lishangbu.avalon.oauth2.common.properties.Oauth2Properties
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.mockito.Answers
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.context.ApplicationContext
import org.springframework.context.support.GenericApplicationContext
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer
import org.springframework.security.web.DefaultSecurityFilterChain
import org.springframework.security.web.SecurityFilterChain
import tools.jackson.databind.json.JsonMapper

class ResourceServerAutoConfigurationTest {
    @Test
    fun buildsSecurityFilterChainWithDefaultHandlers() {
        val properties =
            Oauth2Properties().apply {
                ignoreUrls.add("/public/**")
            }
        val configuration = ResourceServerAutoConfiguration(properties)
        val jsonMapper = JsonMapper()
        val httpSecurity = mock(HttpSecurity::class.java, Answers.RETURNS_SELF)
        val resourceServerConfigurer =
            mock(OAuth2ResourceServerConfigurer::class.java, Answers.RETURNS_SELF) as OAuth2ResourceServerConfigurer<HttpSecurity>
        val exceptionHandlingConfigurer =
            mock(ExceptionHandlingConfigurer::class.java, Answers.RETURNS_SELF) as ExceptionHandlingConfigurer<HttpSecurity>

        Mockito.`when`(httpSecurity.csrf(ArgumentMatchers.any())).thenReturn(httpSecurity)
        Mockito.`when`(httpSecurity.cors(ArgumentMatchers.any())).thenReturn(httpSecurity)
        Mockito.`when`(httpSecurity.formLogin(ArgumentMatchers.any())).thenReturn(httpSecurity)
        Mockito.`when`(httpSecurity.sessionManagement(ArgumentMatchers.any())).thenReturn(httpSecurity)
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
                    invocation.getArgument(0) as Customizer<OAuth2ResourceServerConfigurer<HttpSecurity>>
                customizer.customize(resourceServerConfigurer)
                httpSecurity
            }.`when`(httpSecurity)
            .oauth2ResourceServer(ArgumentMatchers.any())

        Mockito
            .doAnswer { invocation ->
                @Suppress("UNCHECKED_CAST")
                val customizer =
                    invocation.getArgument(0) as Customizer<ExceptionHandlingConfigurer<HttpSecurity>>
                customizer.customize(exceptionHandlingConfigurer)
                httpSecurity
            }.`when`(httpSecurity)
            .exceptionHandling(ArgumentMatchers.any())

        val filterChain = mock(DefaultSecurityFilterChain::class.java)
        Mockito.`when`(httpSecurity.build()).thenReturn(filterChain)

        val result: SecurityFilterChain =
            configuration.resourceServerSecurityFilterChain(httpSecurity, jsonMapper)

        assertSame(filterChain, result)
        verify(resourceServerConfigurer).opaqueToken(ArgumentMatchers.any())
        verify(resourceServerConfigurer).authenticationEntryPoint(ArgumentMatchers.any())
        verify(resourceServerConfigurer).accessDeniedHandler(ArgumentMatchers.any())
        verify(exceptionHandlingConfigurer).authenticationEntryPoint(ArgumentMatchers.any())
        verify(exceptionHandlingConfigurer).accessDeniedHandler(ArgumentMatchers.any())
    }
}
