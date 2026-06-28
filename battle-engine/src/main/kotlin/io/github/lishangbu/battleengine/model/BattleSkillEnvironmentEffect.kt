package io.github.lishangbu.battleengine.model

/**
 * 技能成功后直接改写全场环境的结构化效果。
 *
 * 环境效果与一侧屏障、入场陷阱等“场上状态”分开建模，因为天气和场地会被伤害公式、速度计算、状态免疫、
 * 回合末回复等多个阶段读取。这里不保存技能名或数据库 policy 字符串；资料层负责把稳定 policy 转换成
 * 明确的天气、场地和持续回合。
 */
sealed interface BattleSkillEnvironmentEffect {
	/**
	 * 技能成功后设置全场天气。
	 *
	 * 现代普通天气技能默认持续 5 回合。延长天气的携带道具、强天气覆盖、天气封锁等更复杂交互后续会以
	 * 独立效果或战斗状态接入；该结构只表达“本次技能成功后把天气写成指定值”。
	 */
	data class SetWeather(
		val weather: BattleWeather,
		val turnsRemaining: Int? = 5,
	) : BattleSkillEnvironmentEffect {
		init {
			require(weather != BattleWeather.NONE) { "set weather effect requires an active weather" }
			require(turnsRemaining == null || turnsRemaining > 0) {
				"turnsRemaining must be positive when present"
			}
		}
	}
}
