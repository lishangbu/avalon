package io.github.lishangbu.battleengine.model

/**
 * 当前战斗天气。
 *
 * 这里表达现代主系列常见的全场天气状态。天气本身只是一种环境事实；具体倍率、持续回合、
 * 免疫或伤害副作用由规则快照和状态机分阶段解释，避免在枚举里塞入隐式业务逻辑。
 */
enum class BattleWeather {
	NONE,
	SUN,
	RAIN,
	SANDSTORM,
	SNOW,
}
