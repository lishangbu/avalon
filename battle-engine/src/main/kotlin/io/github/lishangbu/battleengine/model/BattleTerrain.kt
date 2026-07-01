package io.github.lishangbu.battleengine.model

/**
 * 当前战斗场地。
 *
 * 场地只保存当前全场环境类型，是否“接地”由成员快照和特性效果在使用点计算。当前引擎已经接入的现代核心行为包括：
 * 电气场地阻止接地成员新获得睡眠；青草场地提供回合末回复、草属性伤害强化和部分地面类技能伤害减弱；
 * 薄雾场地阻止接地成员新获得主要异常状态；精神场地阻止对接地对手使用先制技能。持续回合和场地切换事件保存在
 * [BattleEnvironment] 与事件流中，枚举本身不承担计时或文本职责。
 */
enum class BattleTerrain {
	NONE,
	ELECTRIC,
	GRASSY,
	MISTY,
	PSYCHIC,
}
