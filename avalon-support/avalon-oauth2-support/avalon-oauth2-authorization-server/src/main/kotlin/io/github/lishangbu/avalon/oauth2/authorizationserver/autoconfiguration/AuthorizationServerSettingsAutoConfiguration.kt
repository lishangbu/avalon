package io.github.lishangbu.avalon.oauth2.authorizationserver.autoconfiguration

import io.github.lishangbu.avalon.oauth2.common.properties.Oauth2Properties
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings
import org.springframework.security.web.util.UrlUtils

/**
 * 授权服务器设置自动配置
 *
 * 根据配置构建默认的 [AuthorizationServerSettings]
 */
@AutoConfiguration
class AuthorizationServerSettingsAutoConfiguration {
    /** 创建授权服务器设置 */
    @Bean
    @ConditionalOnMissingBean
    fun authorizationServerSettings(properties: Oauth2Properties): AuthorizationServerSettings {
        val builder = AuthorizationServerSettings.builder()
        val issuerUrl = properties.issuerUrl
        if (issuerUrl != null && UrlUtils.isAbsoluteUrl(issuerUrl)) {
            builder.issuer(issuerUrl)
        }
        return builder.build()
    }
}
