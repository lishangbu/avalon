package io.github.lishangbu.avalon.standalone

import org.babyfish.jimmer.spring.repository.EnableJimmerRepositories
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * 单体应用启动类
 *
 * Spring Boot 单体应用的入口
 *
 * @author lishangbu
 * @since 2025/8/24
 */
@EnableJimmerRepositories("io.github.lishangbu.avalon.**.repository")
@SpringBootApplication(scanBasePackages = ["io.github.lishangbu.avalon"])
class AvalonStandaloneApplication

fun main(args: Array<String>) {
    runApplication<AvalonStandaloneApplication>(*args)
}
