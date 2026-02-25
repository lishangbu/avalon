package io.github.lishangbu.avalon.s3.autoconfiguration;

import io.github.lishangbu.avalon.s3.properties.S3Properties;
import io.github.lishangbu.avalon.s3.template.S3Template;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/// S3 自动装配
///
/// 根据 `S3Properties` 的配置为应用提供 `S3Template` Bean
/// 当未定义 `S3Template` 且 `s3.enabled=true`（默认）时，自动创建并注入 `S3Template`
///
@AutoConfiguration
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({S3Properties.class})
public class S3AutoConfiguration {
    /// S3 操作模板
    ///
    /// 在未定义自定义 `S3Template` Bean 时，基于 `S3Properties` 创建默认实现
    ///
    /// @param properties S3 配置项
    /// @return 默认的 `S3Template` 实例
    @Bean
    @ConditionalOnMissingBean(S3Template.class)
    @ConditionalOnProperty(
            prefix = S3Properties.PREFIX,
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    public S3Template s3Template(S3Properties properties) {
        return new S3Template(properties);
    }
}
