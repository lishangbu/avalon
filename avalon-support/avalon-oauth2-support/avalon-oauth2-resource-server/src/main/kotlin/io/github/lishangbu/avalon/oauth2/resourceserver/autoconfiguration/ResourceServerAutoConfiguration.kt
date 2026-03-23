package io.github.lishangbu.avalon.oauth2.resourceserver.autoconfiguration

import io.github.lishangbu.avalon.oauth2.common.constant.SecurityBeanDefinitionConstants.RESOURCE_SERVER_SECURITY_FILTER_CHAIN_BEAN_NAME
import io.github.lishangbu.avalon.oauth2.common.constant.SecurityBeanDefinitionConstants.RESOURCE_SERVER_SECURITY_FILTER_CHAIN_BEAN_ORDER
import io.github.lishangbu.avalon.oauth2.common.properties.Oauth2Properties
import io.github.lishangbu.avalon.oauth2.common.web.access.DefaultAccessDeniedHandler
import io.github.lishangbu.avalon.oauth2.common.web.authentication.DefaultAuthenticationEntryPoint
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.core.annotation.Order
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configurers.*
import org.springframework.security.web.SecurityFilterChain
import tools.jackson.databind.json.JsonMapper

/**
 * 资源服务器自动配置
 *
 * 提供默认的资源服务器安全过滤链与统一异常响应
 */
@EnableWebSecurity
@AutoConfiguration
@EnableMethodSecurity(jsr250Enabled = true, securedEnabled = true)
class ResourceServerAutoConfiguration(
    /** OAuth2 属性 */
    private val oauth2Properties: Oauth2Properties,
) {
    /** 创建资源服务器安全过滤链 */
    @Bean
    @ConditionalOnMissingBean(name = [RESOURCE_SERVER_SECURITY_FILTER_CHAIN_BEAN_NAME])
    @Order(RESOURCE_SERVER_SECURITY_FILTER_CHAIN_BEAN_ORDER)
    fun resourceServerSecurityFilterChain(
        http: HttpSecurity,
        jsonMapper: JsonMapper,
    ): SecurityFilterChain {
        http
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers(*oauth2Properties.ignoreUrls.toTypedArray())
                    .permitAll()
                    .anyRequest()
                    .authenticated()
            }.csrf(CsrfConfigurer<HttpSecurity>::disable)
            .cors(CorsConfigurer<HttpSecurity>::disable)
            .formLogin(FormLoginConfigurer<HttpSecurity>::disable)
            .sessionManagement(SessionManagementConfigurer<HttpSecurity>::disable)
            .rememberMe(RememberMeConfigurer<HttpSecurity>::disable)

        http.oauth2ResourceServer { oauth2ResourceServer ->
            oauth2ResourceServer.opaqueToken(Customizer.withDefaults())
            oauth2ResourceServer
                .authenticationEntryPoint(DefaultAuthenticationEntryPoint(jsonMapper))
                .accessDeniedHandler(DefaultAccessDeniedHandler(jsonMapper))
        }

        http.exceptionHandling { exceptions ->
            exceptions
                .authenticationEntryPoint(DefaultAuthenticationEntryPoint(jsonMapper))
                .accessDeniedHandler(DefaultAccessDeniedHandler(jsonMapper))
        }

        return http.build()
    }
}
