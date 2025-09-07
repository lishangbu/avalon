package io.github.lishangbu.avalon.authorization;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * @author lishangbu
 * @since 2025/8/20
 */
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EntityScan(basePackages = "io.github.lishangbu.avalon.authorization.entity")
@EnableJpaRepositories(basePackages = "io.github.lishangbu.avalon.authorization.repository")
public class TestEnvironmentAutoConfiguration {}
