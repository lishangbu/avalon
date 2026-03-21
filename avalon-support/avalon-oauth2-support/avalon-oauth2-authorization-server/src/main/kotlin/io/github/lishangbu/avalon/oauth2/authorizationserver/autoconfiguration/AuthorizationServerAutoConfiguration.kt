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
 * 自动装配认证服务器
 *
 * 提供认证服务器所需的 SecurityFilterChain，集成 OAuth2 授权端点、Token 端点及相关处理器
 *
 * @author lishangbu
 * @since 2025/8/17
 */
@EnableWebSecurity
@AutoConfiguration
@EnableMethodSecurity(jsr250Enabled = true, securedEnabled = true)
class AuthorizationServerAutoConfiguration {
    /**
     * 授权服务器的 SecurityFilterChain Bean
     *
     * 负责注册 OAuth2/OIDC 端点及扩展授权类型（密码、短信、邮箱）对应的转换器与认证提供者
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

        // 禁用csrf和cors
        http
            .csrf(CsrfConfigurer<HttpSecurity>::disable)
            .cors(CorsConfigurer<HttpSecurity>::disable)
            // 禁用表单登录和会话管理
            .formLogin(FormLoginConfigurer<HttpSecurity>::disable)
            .sessionManagement(SessionManagementConfigurer<HttpSecurity>::disable)
            .rememberMe(RememberMeConfigurer<HttpSecurity>::disable)

        val securityFilterChain: DefaultSecurityFilterChain =
            http
                .securityMatcher(authorizationServerConfigurer.endpointsMatcher)
                .with(authorizationServerConfigurer) { authorizationServer ->
                    // Enable OpenID Connect 1.0
                    authorizationServer
                        .authorizationEndpoint { authorizationEndpoint ->
                            // 用于处理已验证的
                            // OAuth2AuthorizationCodeRequestAuthenticationToken 并返回
                            // OAuth2AuthorizationResponse 的
                            // AuthenticationSuccessHandler (后处理器)
                            authorizationEndpoint
                                .authorizationResponseHandler(
                                    AuthorizationEndpointResponseHandler(),
                                ).errorResponseHandler(
                                    AuthorizationEndpointErrorResponseHandler(jsonMapper),
                                )
                        }.oidc(Customizer.withDefaults())
                        // 定制客户端认证失败的处理器
                        .clientAuthentication { clientAuthentication ->
                            clientAuthentication.errorResponseHandler(
                                oauth2ErrorApiResultAuthenticationFailureHandler,
                            )
                        }.tokenEndpoint { tokenEndpoint ->
                            tokenEndpoint
                                // 定制响应成功格式
                                .accessTokenResponseHandler(
                                    accessTokenResponseAuthenticationSuccessHandler,
                                )
                                // 定制响应失败格式
                                .errorResponseHandler(
                                    oauth2ErrorApiResultAuthenticationFailureHandler,
                                )
                                // 在这加上密码模式的转换器
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
                // Redirect to the login page when not authenticated from the
                // authorization endpoint
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

    @Suppress("UNCHECKED_CAST")
    /**
     * 注册扩展授权类型的 AuthenticationProvider（密码、短信、邮箱）
     *
     * 将自定义 Provider 注入到 HttpSecurity，供 token 端点进行认证
     */
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

        // This will add new authentication provider in the list of existing authentication
        // providers.
        http.authenticationProvider(resourceOwnerPasswordAuthenticationProvider)
        http.authenticationProvider(smsAuthenticationProvider)
        http.authenticationProvider(emailAuthenticationProvider)
    }
}
