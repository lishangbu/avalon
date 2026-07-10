package io.github.lishangbu.gamedata

import org.babyfish.jimmer.spring.repository.EnableJimmerRepositories
import org.springframework.context.annotation.Configuration

/**
 * 装配游戏资料模块的 Jimmer Repository。
 */
@Configuration(proxyBeanMethods = false)
@EnableJimmerRepositories(basePackages = ["io.github.lishangbu.gamedata.repository"])
class GameDataConfig
