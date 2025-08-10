package io.github.lishangbu.avalon.auth.configuration;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

/**
 * 测试环境配置
 *
 * @author lishangbu
 * @since 2025/4/11
 */
@ComponentScan(basePackages = "io.github.lishangbu.avalon.data.jdbc.callback")
@EntityScan(basePackages = "io.github.lishangbu.avalon.auth.entity")
@EnableJdbcRepositories(basePackages = "io.github.lishangbu.avalon.auth.repository")
public class AuthRepositoryTestEnvironmentConfiguration {}
