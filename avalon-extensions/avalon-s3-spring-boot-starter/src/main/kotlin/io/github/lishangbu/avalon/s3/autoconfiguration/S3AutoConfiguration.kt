package io.github.lishangbu.avalon.s3.autoconfiguration

import io.github.lishangbu.avalon.s3.properties.S3Properties
import io.github.lishangbu.avalon.s3.template.S3Template
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * S3 自动配置
 *
 * 在启用 S3 配置时提供默认的 [S3Template]
 */
@AutoConfiguration
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(S3Properties::class)
class S3AutoConfiguration {
    /** 创建 S3 操作模板 */
    @Bean
    @ConditionalOnMissingBean(S3Template::class)
    @ConditionalOnProperty(
        prefix = S3Properties.PREFIX,
        name = ["enabled"],
        havingValue = "true",
        matchIfMissing = true,
    )
    fun s3Template(properties: S3Properties): S3Template = S3Template(properties)
}
