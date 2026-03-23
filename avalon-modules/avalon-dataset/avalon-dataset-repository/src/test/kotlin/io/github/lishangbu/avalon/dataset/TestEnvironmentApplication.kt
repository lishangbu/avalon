package io.github.lishangbu.avalon.dataset

import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

/**
 * 数据集模块测试环境配置
 *
 * 为仓储测试提供最小化的 Spring Boot 上下文
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = ["io.github.lishangbu.avalon.dataset.repository"])
class TestEnvironmentApplication {
    /** 创建 PostgreSQL 测试容器 */
    @Bean
    @ServiceConnection
    fun postgresContainer(): PostgreSQLContainer = PostgreSQLContainer(DockerImageName.parse("postgres"))
}
