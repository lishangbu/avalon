package io.github.lishangbu.avalon.idempotent.autoconfigure

import io.github.lishangbu.avalon.idempotent.aspect.IdempotentAspect
import io.github.lishangbu.avalon.idempotent.key.IdempotentKeyResolver
import io.github.lishangbu.avalon.idempotent.store.IdempotentStore
import io.github.lishangbu.avalon.idempotent.store.JdbcIdempotentStore
import io.github.lishangbu.avalon.idempotent.store.RedisIdempotentStore
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import tools.jackson.databind.json.JsonMapper

class IdempotentAutoConfigurationTest {
    private val contextRunner =
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(IdempotentAutoConfiguration::class.java))

    @Test
    fun registersDefaultBeansWhenRedisTemplateExists() {
        contextRunner
            .withBean(StringRedisTemplate::class.java, {
                StringRedisTemplate(mock(RedisConnectionFactory::class.java))
            })
            .run { context ->
                assertThat(context).hasSingleBean(JsonMapper::class.java)
                assertThat(context).hasSingleBean(IdempotentKeyResolver::class.java)
                assertThat(context).hasSingleBean(IdempotentStore::class.java)
                assertThat(context).hasSingleBean(IdempotentAspect::class.java)
                assertThat(context.getBean(IdempotentStore::class.java)).isInstanceOf(RedisIdempotentStore::class.java)
            }
    }

    @Test
    fun backsOffWhenStoreIsProvidedByUser() {
        contextRunner
            .withBean(IdempotentStore::class.java, {
                object : IdempotentStore {
                    override fun acquire(
                        key: String,
                        token: String,
                        processingTtl: java.time.Duration,
                    ): IdempotentStore.AcquireResult = IdempotentStore.AcquireResult.Acquired

                    override fun complete(
                        key: String,
                        token: String,
                        cachedValue: String?,
                        ttl: java.time.Duration,
                    ): Boolean = true

                    override fun release(
                        key: String,
                        token: String,
                    ): Boolean = true

                    override fun renew(
                        key: String,
                        token: String,
                        processingTtl: java.time.Duration,
                    ): Boolean = true
                }
            })
            .run { context ->
                assertThat(context).hasSingleBean(IdempotentStore::class.java)
                assertThat(context).hasSingleBean(IdempotentAspect::class.java)
            }
    }

    @Test
    fun registersJdbcStoreWhenConfigured() {
        contextRunner
            .withPropertyValues("idempotent.store-type=JDBC")
            .withBean(JdbcTemplate::class.java, {
                JdbcTemplate(
                    DriverManagerDataSource().apply {
                        setDriverClassName("org.h2.Driver")
                        url = "jdbc:h2:mem:idempotent-auto-config;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
                        username = "sa"
                    },
                )
            })
            .run { context ->
                assertThat(context).hasSingleBean(IdempotentStore::class.java)
                assertThat(context.getBean(IdempotentStore::class.java)).isInstanceOf(JdbcIdempotentStore::class.java)
                assertThat(context).hasSingleBean(IdempotentAspect::class.java)
            }
    }

    @Test
    fun skipsConfigurationWhenDisabled() {
        contextRunner
            .withPropertyValues("idempotent.enabled=false")
            .withBean(StringRedisTemplate::class.java, {
                StringRedisTemplate(mock(RedisConnectionFactory::class.java))
            })
            .run { context ->
                assertThat(context).doesNotHaveBean(IdempotentAspect::class.java)
                assertThat(context).doesNotHaveBean(IdempotentStore::class.java)
            }
    }
}
