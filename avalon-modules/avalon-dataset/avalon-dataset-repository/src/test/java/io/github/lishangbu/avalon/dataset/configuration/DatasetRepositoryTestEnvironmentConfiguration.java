package io.github.lishangbu.avalon.dataset.configuration;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 测试环境配置
 *
 * @author lishangbu
 * @since 2025/4/11
 */
@EntityScan(basePackages = "io.github.lishangbu.avalon.dataset.entity")
@EnableJpaRepositories(basePackages = "io.github.lishangbu.avalon.dataset.repository")
public class DatasetRepositoryTestEnvironmentConfiguration {}
