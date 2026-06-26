package io.github.lishangbu.security.oauth

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan

/**
 * token endpoint 集成测试使用的最小安全应用。
 */
@SpringBootApplication(scanBasePackages = ["io.github.lishangbu.security"])
@ConfigurationPropertiesScan(basePackages = ["io.github.lishangbu.security"])
class SecurityTokenEndpointTestApplication
