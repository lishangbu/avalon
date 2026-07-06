package io.github.lishangbu.battlerules

import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.ComponentScan

/**
 * 战斗规则模块测试使用的最小 Spring Boot 应用。
 *
 * 这里额外扫描 common-web，是为了让模块级 Controller 测试也使用生产一致的全局异常响应；
 * 否则 Service 抛出的 ApiException 会在 MockMvc 中表现为未处理异常，无法覆盖前端真正依赖的 JSON 错误体。
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan("io.github.lishangbu.battlerules", "io.github.lishangbu.common.web")
class BattleRulesTestApplication
