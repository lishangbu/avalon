package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battleengine.model.BattleSideConditionTarget
import io.github.lishangbu.battleengine.model.BattleWeather

/**
 * 一侧场地效果和场地规则合并后的最小运行时行。
 *
 * `effectPolicy` 决定该行进入屏障、速度修正、入场陷阱或保护效果；持续回合与最大层数只在对应模型中消费。
 */
internal data class SideFieldEffectRuntimeRow(
	val effectPolicy: String,
	val minTurns: Int?,
	val maxLayers: Int?,
	val targetSide: BattleSideConditionTarget,
	val chancePercent: Int,
	val requiredWeather: BattleWeather?,
)
