package io.github.lishangbu.battlerules

import org.babyfish.jimmer.spring.repository.EnableJimmerRepositories
import org.springframework.context.annotation.Configuration

/**
 * 战斗规则模块的 Spring 装配入口。
 *
 * 该配置只负责暴露本模块拥有的 Jimmer Repository。应用启动模块通过依赖 `battle-rules`
 * 获得这些 Bean，安全模块仍只通过稳定权限 code 保护战斗规则 API 路径前缀，避免业务模块反向耦合安全表。
 */
@Configuration(proxyBeanMethods = false)
@EnableJimmerRepositories(basePackages = ["io.github.lishangbu.battlerules.repository"])
class BattleRulesConfig
