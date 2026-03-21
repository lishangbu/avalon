package io.github.lishangbu.avalon.authorization

import io.github.lishangbu.avalon.authorization.service.impl.DefaultOAuth2AuthorizationConsentService
import io.github.lishangbu.avalon.authorization.service.impl.DefaultOAuth2AuthorizationService
import io.github.lishangbu.avalon.authorization.service.impl.DefaultRegisteredClientRepository
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

/**
 * 测试环境自动配置类
 *
 * @author lishangbu
 * @since 2025/8/20
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = ["io.github.lishangbu.avalon.authorization.repository"])
@Import(
    DefaultRegisteredClientRepository::class,
    DefaultOAuth2AuthorizationService::class,
    DefaultOAuth2AuthorizationConsentService::class,
)
class TestEnvironmentApplication {
    /**
     * PostgreSQL 测试容器 Bean
     *
     * 使用 @ServiceConnection 注解，Spring Boot 会自动配置数据源连接
     *
     * @return PostgreSQL 容器实例
     */
    @Bean
    @ServiceConnection
    fun postgresContainer(): PostgreSQLContainer = PostgreSQLContainer(DockerImageName.parse("postgres"))
}
