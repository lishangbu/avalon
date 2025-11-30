package io.github.lishangbu.avalon.authorization;

import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

/**
 * @author lishangbu
 * @since 2025/8/20
 */
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EntityScan(basePackages = "io.github.lishangbu.avalon.authorization.entity")
@EnableJdbcRepositories(basePackages = "io.github.lishangbu.avalon.authorization.repository")
public class TestEnvironmentAutoConfiguration {}
