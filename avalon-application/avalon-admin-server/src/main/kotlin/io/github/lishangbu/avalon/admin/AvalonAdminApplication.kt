package io.github.lishangbu.avalon.admin

import org.babyfish.jimmer.spring.repository.EnableJimmerRepositories
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * 管理应用启动类
 *
 * Spring Boot 管理后台应用的入口
 *
 * @author lishangbu
 * @since 2025/8/24
 */
@EnableJimmerRepositories("io.github.lishangbu.avalon.**.repository")
@SpringBootApplication(scanBasePackages = ["io.github.lishangbu.avalon"])
class AvalonAdminApplication

fun main(args: Array<String>) {
    runApplication<AvalonAdminApplication>(*args)
}
