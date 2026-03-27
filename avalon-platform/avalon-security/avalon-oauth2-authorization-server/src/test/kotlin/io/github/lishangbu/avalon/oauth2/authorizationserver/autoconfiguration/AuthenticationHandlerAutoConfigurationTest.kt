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
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.ObjectProvider
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.transaction.PlatformTransactionManager
import tools.jackson.databind.json.JsonMapper
import java.util.function.Supplier

class AuthenticationHandlerAutoConfigurationTest {
    @Test
    fun createsLoginFailureTrackerWithDefaults() {
        val configuration = AuthenticationHandlerAutoConfiguration()

        val tracker: LoginFailureTracker =
            configuration.loginFailureTracker(
                providerOf<Oauth2Properties>(null),
                providerOf<StringRedisTemplate>(null),
                providerOf<JdbcTemplate>(null),
                providerOf<PlatformTransactionManager>(null),
            )

        assertTrue(tracker is InMemoryLoginFailureTracker)
        assertFalse(tracker.isEnabled())
    }

    @Test
    fun createsRedisLoginFailureTrackerWhenConfigured() {
        val configuration = AuthenticationHandlerAutoConfiguration()
        val properties =
            Oauth2Properties().apply {
                loginFailureTrackerStoreType = LoginFailureTrackerStoreType.REDIS
            }

        val tracker =
            configuration.loginFailureTracker(
                providerOf(properties),
                providerOf(Mockito.mock(StringRedisTemplate::class.java)),
                providerOf<JdbcTemplate>(null),
                providerOf<PlatformTransactionManager>(null),
            )

        assertTrue(tracker is RedisLoginFailureTracker)
    }

    @Test
    fun createsJdbcLoginFailureTrackerWhenConfigured() {
        val configuration = AuthenticationHandlerAutoConfiguration()
        val properties =
            Oauth2Properties().apply {
                loginFailureTrackerStoreType = LoginFailureTrackerStoreType.JDBC
            }

        val tracker =
            configuration.loginFailureTracker(
                providerOf(properties),
                providerOf<StringRedisTemplate>(null),
                providerOf(Mockito.mock(JdbcTemplate::class.java)),
                providerOf(Mockito.mock(PlatformTransactionManager::class.java)),
            )

        assertTrue(tracker is JdbcLoginFailureTracker)
    }

    @Test
    fun failsFastWhenRedisTrackerIsMissingTemplate() {
        val configuration = AuthenticationHandlerAutoConfiguration()
        val properties =
            Oauth2Properties().apply {
                loginFailureTrackerStoreType = LoginFailureTrackerStoreType.REDIS
            }

        val exception =
            assertThrows(IllegalStateException::class.java) {
                configuration.loginFailureTracker(
                    providerOf(properties),
                    providerOf<StringRedisTemplate>(null),
                    providerOf<JdbcTemplate>(null),
                    providerOf<PlatformTransactionManager>(null),
                )
            }

        assertTrue(exception.message!!.contains("StringRedisTemplate"))
    }

    @Test
    fun failsFastWhenJdbcTrackerIsMissingInfrastructure() {
        val configuration = AuthenticationHandlerAutoConfiguration()
        val properties =
            Oauth2Properties().apply {
                loginFailureTrackerStoreType = LoginFailureTrackerStoreType.JDBC
            }

        val exception =
            assertThrows(IllegalStateException::class.java) {
                configuration.loginFailureTracker(
                    providerOf(properties),
                    providerOf<StringRedisTemplate>(null),
                    providerOf<JdbcTemplate>(null),
                    providerOf<PlatformTransactionManager>(null),
                )
            }

        assertTrue(exception.message!!.contains("JdbcTemplate"))
    }

    @Test
    fun createsHandlersUsingProvidedDependencies() {
        val configuration = AuthenticationHandlerAutoConfiguration()
        val recorder = AuthenticationLogRecorder {}
        val authorizationService = Mockito.mock(OAuth2AuthorizationService::class.java)
        val properties = Oauth2Properties()
        val jsonMapper = JsonMapper()

        val successHandler: OAuth2AccessTokenApiResultResponseAuthenticationSuccessHandler =
            configuration.accessTokenResponseAuthenticationSuccessHandler(
                providerOf(recorder),
                providerOf(authorizationService),
                providerOf(properties),
                jsonMapper,
            )
        val failureHandler: OAuth2ErrorApiResultAuthenticationFailureHandler =
            configuration.oauth2ErrorApiResultAuthenticationFailureHandler(
                providerOf(recorder),
                providerOf(properties),
                jsonMapper,
            )

        assertNotNull(successHandler)
        assertNotNull(failureHandler)
    }

    private fun <T : Any> providerOf(value: T?): ObjectProvider<T> =
        object : ObjectProvider<T> {
            override fun getObject(): T = value ?: throw NoSuchElementException()

            override fun getObject(vararg args: Any?): T = value ?: throw NoSuchElementException()

            override fun getIfAvailable(): T? = value

            override fun getIfAvailable(supplier: Supplier<T>): T = value ?: supplier.get()

            override fun iterator(): MutableIterator<T> =
                if (value == null) {
                    mutableListOf<T>().iterator()
                } else {
                    mutableListOf(value).iterator()
                }
        }
}
