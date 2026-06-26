package io.github.lishangbu

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

/**
 * 后端的运行时装配入口。
 *
 * 应用模块只负责启动 Spring Boot、扫描配置属性并组合安全模块，不承载具体业务规则。
 */
@SpringBootApplication
@ConfigurationPropertiesScan
class BackendApplication

/**
 * 启动 模块化单体应用。
 */
fun main(args: Array<String>) {
	runApplication<BackendApplication>(*args)
}
