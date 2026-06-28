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
	 * 现代普通天气技能默认持续 5 回合。携带道具延长持续时间由状态机在写入天气前读取使用者道具效果；
	 * 强天气覆盖、天气封锁等更复杂交互后续会以独立效果或战斗状态接入。该结构只表达“本次技能成功后把天气
	 * 写成指定值”，不直接保存具体道具或技能名称。
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

	/**
	 * 技能成功后设置全场场地。
	 *
	 * 现代普通场地技能默认持续 5 回合。携带道具延长持续时间由状态机在写入场地前读取使用者道具效果；
	 * 已有相同场地下的失败语义、场地覆盖限制等复杂交互会作为独立规则逐步接入。该结构只承载“成功后把全场
	 * 场地写成指定值”的事实。
	 */
	data class SetTerrain(
		val terrain: BattleTerrain,
		val turnsRemaining: Int? = 5,
	) : BattleSkillEnvironmentEffect {
		init {
			require(terrain != BattleTerrain.NONE) { "set terrain effect requires an active terrain" }
			require(turnsRemaining == null || turnsRemaining > 0) {
				"turnsRemaining must be positive when present"
			}
		}
	}
}
