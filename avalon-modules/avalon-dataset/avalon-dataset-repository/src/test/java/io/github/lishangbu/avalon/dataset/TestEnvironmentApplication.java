package io.github.lishangbu.avalon.dataset;

import io.github.lishangbu.avalon.mybatisplus.autoconfiguration.MybatisPlusAutoConfiguration;
import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/// 测试环境自动配置类
///
/// 通过 Java 代码配置测试环境，包含 Liquibase 数据库迁移和 MyBatis-Plus 配置，避免使用外部 application.yml
/// 配置文件，提高测试的可移植性
///
/// 配置顺序：
///
/// 1. PostgreSQL 容器启动并暴露连接信息
/// 2. @ServiceConnection 自动配置 DataSource
/// 3. LiquibaseAutoConfiguration 自动执行数据库迁移脚本
/// 4. MybatisPlusAutoConfiguration 初始化 MyBatis-Plus
///
/// 所有 Mapper 测试通过继承 AbstractMapperTest 自动获取此配置
///
/// @author lishangbu
/// @since 2025/8/20
@Import(value = {MybatisPlusAutoConfiguration.class})
@Configuration(proxyBeanMethods = false)
public class TestEnvironmentApplication {

  /// PostgreSQL 测试容器 Bean
  ///
  /// 使用 @ServiceConnection 注解，Spring Boot 会自动配置数据源连接
  /// 容器在返回前已启动，确保 Liquibase 可以立即连接数据库
  ///
  /// @return 已启动的 PostgreSQL 容器实例
  @Bean
  @ServiceConnection
  PostgreSQLContainer postgresContainer() {
    PostgreSQLContainer container =
        new PostgreSQLContainer(DockerImageName.parse("postgres:latest"));
    container.start();
    return container;
  }

  /// 配置 Liquibase Bean，确保数据库迁移在测试前完成
  ///
  /// 通过显式配置 Liquibase Bean，可以确保数据库表结构在测试执行前已经创建完成
  /// 其他需要数据库的 Bean 可以通过 @DependsOn("liquibase") 确保在 Liquibase 之后初始化
  ///
  /// @param dataSource 数据源，由 @ServiceConnection 自动配置
  /// @return Liquibase 实例
  @Bean
  public SpringLiquibase liquibase(DataSource dataSource) {
    SpringLiquibase liquibase = new SpringLiquibase();
    liquibase.setDataSource(dataSource);
    liquibase.setChangeLog("classpath:/db/changelog/db.changelog-master.yaml");
    liquibase.setShouldRun(true);
    return liquibase;
  }
}
