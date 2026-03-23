package io.github.lishangbu.avalon.oauth2.authorizationserver.autoconfiguration

import io.github.lishangbu.avalon.oauth2.authorizationserver.login.LoginFailureTracker
import io.github.lishangbu.avalon.oauth2.authorizationserver.web.authentication.AuthorizationEndpointErrorResponseHandler
import io.github.lishangbu.avalon.oauth2.authorizationserver.web.authentication.AuthorizationEndpointResponseHandler
import io.github.lishangbu.avalon.oauth2.authorizationserver.web.authentication.OAuth2AccessTokenApiResultResponseAuthenticationSuccessHandler
import io.github.lishangbu.avalon.oauth2.authorizationserver.web.authentication.OAuth2ErrorApiResultAuthenticationFailureHandler
import io.github.lishangbu.avalon.oauth2.common.constant.SecurityBeanDefinitionConstants.AUTHORIZATION_SERVER_SECURITY_FILTER_CHAIN_BEAN_NAME
import io.github.lishangbu.avalon.oauth2.common.constant.SecurityBeanDefinitionConstants.AUTHORIZATION_SERVER_SECURITY_FILTER_CHAIN_BEAN_ORDER
import io.github.lishangbu.avalon.oauth2.common.properties.Oauth2Properties
import io.github.lishangbu.avalon.oauth2.common.web.authentication.DefaultAuthenticationEntryPoint
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.core.annotation.Order
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configurers.*
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer
import org.springframework.security.oauth2.core.OAuth2Token
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2EmailAuthenticationProvider
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2PasswordAuthenticationProvider
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2SmsAuthenticationProvider
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator
import org.springframework.security.oauth2.server.authorization.web.authentication.*
import org.springframework.security.web.DefaultSecurityFilterChain
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.DelegatingAuthenticationConverter
import tools.jackson.databind.json.JsonMapper

/**
 * 授权服务器自动配置
 *
 * 提供授权端点、令牌端点和扩展授权类型所需的安全过滤链
 */
@EnableWebSecurity
@AutoConfiguration
@EnableMethodSecurity(jsr250Enabled = true, securedEnabled = true)
class AuthorizationServerAutoConfiguration {
    /**
     * 创建授权服务器安全过滤链
     */
    @Bean
    @Order(AUTHORIZATION_SERVER_SECURITY_FILTER_CHAIN_BEAN_ORDER)
    @ConditionalOnMissingBean(name = [AUTHORIZATION_SERVER_SECURITY_FILTER_CHAIN_BEAN_NAME])
    @Throws(Exception::class)
    fun authorizationServerSecurityFilterChain(
        http: HttpSecurity,
        oauth2Properties: Oauth2Properties,
        accessTokenResponseAuthenticationSuccessHandler: OAuth2AccessTokenApiResultResponseAuthenticationSuccessHandler,
        oauth2ErrorApiResultAuthenticationFailureHandler: OAuth2ErrorApiResultAuthenticationFailureHandler,
        loginFailureTracker: LoginFailureTracker,
        jsonMapper: JsonMapper,
    ): SecurityFilterChain {
        val authorizationServerConfigurer = OAuth2AuthorizationServerConfigurer()

        // 禁用 CSRF 和 CORS
        http
            .csrf(CsrfConfigurer<HttpSecurity>::disable)
            .cors(CorsConfigurer<HttpSecurity>::disable)
            // 禁用表单登录、会话管理和记住我
            .formLogin(FormLoginConfigurer<HttpSecurity>::disable)
            .sessionManagement(SessionManagementConfigurer<HttpSecurity>::disable)
            .rememberMe(RememberMeConfigurer<HttpSecurity>::disable)

        val securityFilterChain: DefaultSecurityFilterChain =
            http
                .securityMatcher(authorizationServerConfigurer.endpointsMatcher)
                .with(authorizationServerConfigurer) { authorizationServer ->
                    // 启用 OpenID Connect 1.0
                    authorizationServer
                        .authorizationEndpoint { authorizationEndpoint ->
                            authorizationEndpoint
                                .authorizationResponseHandler(
                                    AuthorizationEndpointResponseHandler(),
                                ).errorResponseHandler(
                                    AuthorizationEndpointErrorResponseHandler(jsonMapper),
                                )
                        }.oidc(Customizer.withDefaults())
                        // 统一客户端认证失败响应
                        .clientAuthentication { clientAuthentication ->
                            clientAuthentication.errorResponseHandler(
                                oauth2ErrorApiResultAuthenticationFailureHandler,
                            )
                        }.tokenEndpoint { tokenEndpoint ->
                            tokenEndpoint
                                // 定制成功响应
                                .accessTokenResponseHandler(
                                    accessTokenResponseAuthenticationSuccessHandler,
                                )
                                // 定制失败响应
                                .errorResponseHandler(
                                    oauth2ErrorApiResultAuthenticationFailureHandler,
                                )
                                // 注册扩展授权类型转换器
                                .accessTokenRequestConverter(
                                    DelegatingAuthenticationConverter(
                                        listOf(
                                            OAuth2AuthorizationCodeAuthenticationConverter(),
                                            OAuth2RefreshTokenAuthenticationConverter(),
                                            OAuth2ClientCredentialsAuthenticationConverter(),
                                            OAuth2PasswordAuthenticationConverter(oauth2Properties),
                                            OAuth2SmsAuthenticationConverter(oauth2Properties),
                                            OAuth2EmailAuthenticationConverter(oauth2Properties),
                                        ),
                                    ),
                                )
                        }
                }.authorizeHttpRequests { authorize -> authorize.anyRequest().authenticated() }
                .oauth2Login { oauth2Login ->
                    oauth2Login
                        .successHandler(AuthorizationEndpointResponseHandler())
                        .failureHandler(AuthorizationEndpointErrorResponseHandler(jsonMapper))
                }.exceptionHandling { exceptions ->
                    // 使用统一的 AuthenticationEntryPoint，避免 MediaTypeRequestMatcher
                    // 导致的不匹配
                    exceptions.authenticationEntryPoint(DefaultAuthenticationEntryPoint(jsonMapper))
                }.build()

        addAdditionalAuthenticationProvider(http, loginFailureTracker)
        return securityFilterChain
    }

    /**
     * 注册扩展授权类型的认证提供者
     */
    @Suppress("UNCHECKED_CAST")
    private fun addAdditionalAuthenticationProvider(
        http: HttpSecurity,
        loginFailureTracker: LoginFailureTracker,
    ) {
        val authenticationManager = http.getSharedObject(AuthenticationManager::class.java)
        val authorizationService = http.getSharedObject(OAuth2AuthorizationService::class.java)
        val tokenGenerator =
            http.getSharedObject(OAuth2TokenGenerator::class.java)
                as OAuth2TokenGenerator<out OAuth2Token>

        val resourceOwnerPasswordAuthenticationProvider =
            OAuth2PasswordAuthenticationProvider(
                authenticationManager,
                authorizationService,
                tokenGenerator,
                loginFailureTracker,
            )

        val smsAuthenticationProvider =
            OAuth2SmsAuthenticationProvider(
                authenticationManager,
                authorizationService,
                tokenGenerator,
                loginFailureTracker,
            )

        val emailAuthenticationProvider =
            OAuth2EmailAuthenticationProvider(
                authenticationManager,
                authorizationService,
                tokenGenerator,
                loginFailureTracker,
            )

        // 将扩展认证提供者追加到现有认证链
        http.authenticationProvider(resourceOwnerPasswordAuthenticationProvider)
        http.authenticationProvider(smsAuthenticationProvider)
        http.authenticationProvider(emailAuthenticationProvider)
    }
}
