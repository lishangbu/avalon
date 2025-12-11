package io.github.lishangbu.avalon.authorization;

import io.github.lishangbu.avalon.mybatisplus.autoconfiguration.MybatisPlusAutoConfiguration;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * 测试环境自动配置类
 *
 * <p>通过 Java 代码配置测试环境，Liquibase 数据库迁移和 MyBatis-Plus 配置 避免使用外部 application.yml 配置文件
 *
 * @author lishangbu
 * @since 2025/8/20
 */
@Import(value = {LiquibaseAutoConfiguration.class, MybatisPlusAutoConfiguration.class})
public class TestEnvironmentApplication {}
