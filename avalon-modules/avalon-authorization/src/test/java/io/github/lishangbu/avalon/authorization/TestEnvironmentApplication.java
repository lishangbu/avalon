package io.github.lishangbu.avalon.authorization;

import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/// 测试环境自动配置类
///
/// @author lishangbu
/// @since 2025/8/20
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EntityScan(basePackages = "io.github.lishangbu.avalon.authorization.entity")
@EnableJpaRepositories(basePackages = "io.github.lishangbu.avalon.authorization.repository")
public class TestEnvironmentApplication {

    /// PostgreSQL 测试容器 Bean
    ///
    /// 使用 @ServiceConnection 注解，Spring Boot 会自动配置数据源连接
    ///
    /// @return PostgreSQL 容器实例
    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres"));
    }
}
