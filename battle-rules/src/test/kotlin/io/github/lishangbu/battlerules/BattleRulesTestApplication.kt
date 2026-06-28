package io.github.lishangbu.battlerules

import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.ComponentScan

/**
 * 战斗规则模块测试使用的最小 Spring Boot 应用。
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan("io.github.lishangbu.battlerules")
class BattleRulesTestApplication
