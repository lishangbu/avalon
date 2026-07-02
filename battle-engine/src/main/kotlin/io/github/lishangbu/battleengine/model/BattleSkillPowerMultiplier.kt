package io.github.lishangbu.battleengine.model

/**
 * 技能进入普通伤害公式前的条件威力倍率。
 *
 * 这些规则都只改变“本次公式使用的技能威力”，不改变技能属性、命中、目标范围或命中后的附加效果。把它们做成
 * 结构化模型，是为了让硬撑、盐水、毒液冲击、祸不单行、杂技等现代规则共享同一个公式入口，同时避免在伤害
 * 计算器里识别具体技能名称。
 *
 * 每个倍率对象只表达一个可复盘条件：
 * - 使用者处于指定主要异常时翻倍。
 * - 目标处于指定主要异常时翻倍。
 * - 目标当前 HP 不高于指定比例时翻倍。
 * - 使用者当前没有有效携带道具时翻倍。
 * - 指定场地存在时应用倍率。
 * - 指定场地存在且目标接地时应用倍率。
 *
 * 滚动、辅助力量、惩罚等会按连续回合或能力阶级累计改变威力的技能，需要额外读取更复杂的运行态，后续会用新的
 * 明确模型接入，不塞进这里的固定倍率条件。
 */
sealed interface BattleSkillPowerMultiplier {
	val multiplier: Double

	/**
	 * 使用者处于指定主要异常状态时应用倍率。
	 *
	 * `statuses` 必须非空，且由资料层明确列出，避免把“任意异常”和“只有中毒/麻痹/灼伤”混为一谈。
	 */
	data class UserMajorStatus(
		val statuses: Set<BattleMajorStatus>,
		override val multiplier: Double,
	) : BattleSkillPowerMultiplier {
		init {
			require(statuses.isNotEmpty()) { "statuses must not be empty" }
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/**
	 * 目标处于指定主要异常状态时应用倍率。
	 *
	 * 毒液冲击只读取普通中毒/剧毒，祸不单行读取任意主要异常；两者都可以由这个模型表达，只是 `statuses`
	 * 集合不同。
	 */
	data class TargetMajorStatus(
		val statuses: Set<BattleMajorStatus>,
		override val multiplier: Double,
	) : BattleSkillPowerMultiplier {
		init {
			require(statuses.isNotEmpty()) { "statuses must not be empty" }
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/**
	 * 目标当前 HP 不高于最大 HP 指定比例时应用倍率。
	 *
	 * 比例使用整数分子分母保存，避免浮点比较造成边界漂移。调用方会用交叉相乘判断，例如 `currentHp * 2 <= maxHp`
	 * 表达“当前 HP 不高于一半”。
	 */
	data class TargetCurrentHpAtMostFraction(
		val numerator: Int,
		val denominator: Int,
		override val multiplier: Double,
	) : BattleSkillPowerMultiplier {
		init {
			require(numerator > 0) { "numerator must be positive" }
			require(denominator > 0) { "denominator must be positive" }
			require(numerator <= denominator) { "numerator must not exceed denominator" }
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/**
	 * 使用者当前没有有效携带道具时应用倍率。
	 *
	 * 这里读取的是运行态中的 `itemId == null`：道具被消耗后会清空该字段，因此杂技这类规则会自然在后续回合生效。
	 */
	data class UserHasNoHeldItem(
		override val multiplier: Double,
	) : BattleSkillPowerMultiplier {
		init {
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/**
	 * 指定场地存在时应用倍率。
	 *
	 * 该模型只读取全场地形，不读取使用者或目标是否接地；它适合那些公开规则明确写作“只要某场地存在就增强威力”
	 * 的技能。若规则需要判断目标是否真的受到场地影响，应使用 [TargetGroundedTerrain]。
	 */
	data class ActiveTerrain(
		val terrain: BattleTerrain,
		override val multiplier: Double,
	) : BattleSkillPowerMultiplier {
		init {
			require(terrain != BattleTerrain.NONE) { "terrain multiplier requires an active terrain" }
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/**
	 * 指定场地存在且目标接地时应用倍率。
	 *
	 * 该模型服务于“目标处于某场地时威力翻倍”的技能规则。它读取目标是否接地，而不是使用者是否接地；使用者是否
	 * 获得场地本身的同属性伤害加成由普通场地伤害倍率独立处理，二者在现代规则下可以同时叠加。
	 */
	data class TargetGroundedTerrain(
		val terrain: BattleTerrain,
		override val multiplier: Double,
	) : BattleSkillPowerMultiplier {
		init {
			require(terrain != BattleTerrain.NONE) { "terrain multiplier requires an active terrain" }
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}
}
