package io.github.lishangbu.battleengine.model

/**
 * 技能命中后尝试建立全场速度顺序效果。
 *
 * 该 application 描述“这次技能如何建立全场效果”，[BattleFieldSpeedOrderEffect] 描述“效果建立后如何参与
 * 行动排序”。两者分离后，同一个全场速度规则可以由不同技能、概率或前置条件复用，而核心引擎仍只处理强类型模型。
 *
 * 现代规则中，戏法空间再次使用时会让已存在的空间效果结束，而不是刷新持续回合；这个重启语义由引擎在应用
 * application 时处理，资料层只负责声明它建立的效果种类与初始持续回合。
 */
data class BattleFieldSpeedOrderApplication(
	val speedOrderEffect: BattleFieldSpeedOrderEffect,
	val chancePercent: Int = 100,
	val requiredWeather: BattleWeather? = null,
) {
	init {
		require(chancePercent in 0..100) { "chancePercent must be between 0 and 100" }
		require(requiredWeather != BattleWeather.NONE) { "requiredWeather cannot be NONE" }
	}
}
