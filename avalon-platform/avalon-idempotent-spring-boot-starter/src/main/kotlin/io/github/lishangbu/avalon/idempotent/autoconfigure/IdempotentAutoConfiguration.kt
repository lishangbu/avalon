package io.github.lishangbu.avalon.idempotent.autoconfigure

import io.github.lishangbu.avalon.idempotent.aspect.IdempotentAspect
import io.github.lishangbu.avalon.idempotent.key.IdempotentKeyResolver
import io.github.lishangbu.avalon.idempotent.key.SpelIdempotentKeyResolver
import io.github.lishangbu.avalon.idempotent.lease.DefaultIdempotentLeaseManager
import io.github.lishangbu.avalon.idempotent.lease.IdempotentLeaseManager
import io.github.lishangbu.avalon.idempotent.properties.IdempotentProperties
import io.github.lishangbu.avalon.idempotent.store.IdempotentStore
import io.github.lishangbu.avalon.idempotent.store.JdbcIdempotentStore
import io.github.lishangbu.avalon.idempotent.store.RedisIdempotentStore
import org.aspectj.lang.ProceedingJoinPoint
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.jdbc.core.JdbcTemplate
import tools.jackson.databind.json.JsonMapper
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

/**
 * Auto-configures Redis-backed idempotent execution support.
 */
@AutoConfiguration
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(ProceedingJoinPoint::class)
@ConditionalOnProperty(
    prefix = IdempotentProperties.PREFIX,
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
@EnableConfigurationProperties(IdempotentProperties::class)
class IdempotentAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(JsonMapper::class)
    fun idempotentJsonMapper(): JsonMapper = JsonMapper.builder().findAndAddModules().build()

    @Bean
    @ConditionalOnMissingBean(IdempotentKeyResolver::class)
    fun idempotentKeyResolver(properties: IdempotentProperties): IdempotentKeyResolver = SpelIdempotentKeyResolver(properties)

    @Bean
    @ConditionalOnBean(StringRedisTemplate::class)
    @ConditionalOnMissingBean(IdempotentStore::class)
    @ConditionalOnProperty(
        prefix = IdempotentProperties.PREFIX,
        name = ["store-type"],
        havingValue = "REDIS",
        matchIfMissing = true,
    )
    fun idempotentStore(
        stringRedisTemplate: StringRedisTemplate,
        jsonMapper: JsonMapper,
    ): IdempotentStore = RedisIdempotentStore(stringRedisTemplate, jsonMapper)

    @Bean
    @ConditionalOnBean(JdbcTemplate::class)
    @ConditionalOnMissingBean(IdempotentStore::class)
    @ConditionalOnProperty(
        prefix = IdempotentProperties.PREFIX,
        name = ["store-type"],
        havingValue = "JDBC",
    )
    fun jdbcIdempotentStore(
        properties: IdempotentProperties,
        jdbcTemplate: JdbcTemplate,
    ): IdempotentStore = JdbcIdempotentStore(jdbcTemplate, properties.jdbcTableName)

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnBean(IdempotentStore::class)
    @ConditionalOnMissingBean(name = ["idempotentLeaseExecutor"])
    fun idempotentLeaseExecutor(): ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "idempotent-renewal").apply {
                isDaemon = true
            }
        }

    @Bean
    @ConditionalOnBean(IdempotentStore::class)
    @ConditionalOnMissingBean(IdempotentLeaseManager::class)
    fun idempotentLeaseManager(
        properties: IdempotentProperties,
        idempotentStore: IdempotentStore,
        idempotentLeaseExecutor: ScheduledExecutorService,
    ): IdempotentLeaseManager = DefaultIdempotentLeaseManager(properties, idempotentStore, idempotentLeaseExecutor)

    @Bean
    @ConditionalOnBean(IdempotentStore::class)
    @ConditionalOnMissingBean
    fun idempotentAspect(
        properties: IdempotentProperties,
        keyResolver: IdempotentKeyResolver,
        idempotentLeaseManager: IdempotentLeaseManager,
        idempotentStore: IdempotentStore,
        jsonMapper: JsonMapper,
    ): IdempotentAspect = IdempotentAspect(properties, keyResolver, idempotentLeaseManager, idempotentStore, jsonMapper)
}
