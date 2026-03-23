package io.github.lishangbu.avalon.game.calculator

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.ComponentScan

/**
 * 游戏计算器自动配置
 *
 * 注册游戏计算相关组件
 */
@AutoConfiguration
@ComponentScan(basePackages = ["io.github.lishangbu.avalon.game.calculator"])
class GameCalculatorAutoConfiguration
