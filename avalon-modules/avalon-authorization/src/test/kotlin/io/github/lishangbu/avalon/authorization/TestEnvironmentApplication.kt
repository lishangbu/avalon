package io.github.lishangbu.avalon.authorization

import io.github.lishangbu.avalon.authorization.service.impl.DefaultOAuth2AuthorizationConsentService
import io.github.lishangbu.avalon.authorization.service.impl.DefaultOAuth2AuthorizationService
import io.github.lishangbu.avalon.authorization.service.impl.DefaultRegisteredClientRepository
import org.babyfish.jimmer.spring.repository.EnableJimmerRepositories
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

/**
 * 授权模块测试环境配置
 *
 * 为仓储测试提供最小化的 Spring Boot 上下文
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@EnableJimmerRepositories("io.github.lishangbu.avalon.authorization.repository")
@ComponentScan(basePackages = ["io.github.lishangbu.avalon.authorization.repository"])
@Import(
    DefaultRegisteredClientRepository::class,
    DefaultOAuth2AuthorizationService::class,
    DefaultOAuth2AuthorizationConsentService::class,
)
class TestEnvironmentApplication {
    /** 创建 PostgreSQL 测试容器 */
    @Bean
    @ServiceConnection
    fun postgresContainer(): PostgreSQLContainer = PostgreSQLContainer(DockerImageName.parse("postgres"))
}
