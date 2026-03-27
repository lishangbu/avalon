package io.github.lishangbu.avalon.oauth2.authorizationserver.autoconfiguration

import io.github.lishangbu.avalon.oauth2.authorizationserver.login.InMemoryLoginFailureTracker
import io.github.lishangbu.avalon.oauth2.authorizationserver.login.JdbcLoginFailureTracker
import io.github.lishangbu.avalon.oauth2.authorizationserver.login.LoginFailureTracker
import io.github.lishangbu.avalon.oauth2.authorizationserver.login.RedisLoginFailureTracker
import io.github.lishangbu.avalon.oauth2.authorizationserver.web.authentication.OAuth2AccessTokenApiResultResponseAuthenticationSuccessHandler
import io.github.lishangbu.avalon.oauth2.authorizationserver.web.authentication.OAuth2ErrorApiResultAuthenticationFailureHandler
import io.github.lishangbu.avalon.oauth2.common.log.AuthenticationLogRecorder
import io.github.lishangbu.avalon.oauth2.common.properties.LoginFailureTrackerStoreType
import io.github.lishangbu.avalon.oauth2.common.properties.Oauth2Properties
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.transaction.PlatformTransactionManager
import tools.jackson.databind.json.JsonMapper

/**
 * 认证处理器自动配置
 *
 * 提供登录失败跟踪器以及统一的认证成功和失败处理器
 */
@AutoConfiguration
class AuthenticationHandlerAutoConfiguration {
    /** 创建登录失败跟踪器 */
    @Bean
    @ConditionalOnMissingBean
    fun loginFailureTracker(
        oauth2PropertiesProvider: ObjectProvider<Oauth2Properties>,
        stringRedisTemplateProvider: ObjectProvider<StringRedisTemplate>,
        jdbcTemplateProvider: ObjectProvider<JdbcTemplate>,
        transactionManagerProvider: ObjectProvider<PlatformTransactionManager>,
    ): LoginFailureTracker {
        val properties = oauth2PropertiesProvider.ifAvailable
        return when (properties?.loginFailureTrackerStoreType ?: LoginFailureTrackerStoreType.MEMORY) {
            LoginFailureTrackerStoreType.MEMORY -> {
                InMemoryLoginFailureTracker(properties)
            }

            LoginFailureTrackerStoreType.REDIS -> {
                RedisLoginFailureTracker(
                    properties,
                    stringRedisTemplateProvider.ifAvailable
                        ?: error("`oauth2.login-failure-tracker-store-type=REDIS` requires a `StringRedisTemplate` bean."),
                )
            }

            LoginFailureTrackerStoreType.JDBC -> {
                JdbcLoginFailureTracker(
                    properties,
                    jdbcTemplateProvider.ifAvailable
                        ?: error("`oauth2.login-failure-tracker-store-type=JDBC` requires a `JdbcTemplate` bean."),
                    transactionManagerProvider.ifAvailable
                        ?: error("`oauth2.login-failure-tracker-store-type=JDBC` requires a `PlatformTransactionManager` bean."),
                )
            }
        }
    }

    /** 创建访问令牌认证成功处理器 */
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

    /** 创建 OAuth2 认证失败处理器 */
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
