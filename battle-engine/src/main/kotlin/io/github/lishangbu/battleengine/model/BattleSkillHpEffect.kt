package io.github.lishangbu.battleengine.model

/**
 * 技能在命中后直接改变 HP 的结构化效果。
 *
 * 这里刻意不把技能名称或本地化描述写进引擎，而是把资料层的 policy code 转换成可复盘的数值规则。
 * 当前覆盖现代主系列中最稳定、最常见的 HP 效果：
 * - 造成普通伤害后，使用者按本次实际伤害的一定比例回复。
 * - 造成普通伤害后，使用者按本次实际伤害的一定比例承受反作用伤害。
 * - 变化技能成功后，使用者按自身最大 HP 的一定比例回复。
 * - 变化技能成功后，按当前天气选择最大 HP 回复比例。
 * - 变化技能成功后，实际目标按目标最大 HP 的一定比例回复。
 * - 变化技能成功后，按当前场地选择目标最大 HP 回复比例。
 * - 变化技能成功后，支付最大 HP 的一定比例建立替身。
 *
 * 吸取回复强化、污泥浆反转、回复封锁、许愿等带有额外状态或更复杂延迟行为的规则，会以新的明确效果继续扩展，
 * 而不是把条件藏进自由文本。
 */
sealed interface BattleSkillHpEffect {
	/**
	 * HP 比例值。
	 *
	 * 作为独立值对象而不是两个散落的数字使用，方便天气变量回复这类规则用 `Map<BattleWeather, HpFraction>`
	 * 精确表达“在某个天气下改用另一个比例”。比例必须是正数且不超过 1，避免资料层把扣血或超量回复误塞进这里。
	 */
	data class HpFraction(
		val numerator: Int,
		val denominator: Int,
	) {
		init {
			require(numerator > 0) { "numerator must be positive" }
			require(denominator > 0) { "denominator must be positive" }
			require(numerator <= denominator) { "numerator must not exceed denominator" }
		}
	}

	/**
	 * 造成伤害后按实际伤害比例回复使用者。
	 *
	 * `numerator / denominator` 表达回复比例，例如 1/2 表示回复本次造成伤害的一半。调用方会按当前缺失 HP
	 * 夹取最终回复量；当伤害为 0 或使用者已经倒下时不会触发。
	 */
	data class DrainDamage(
		val numerator: Int,
		val denominator: Int,
	) : BattleSkillHpEffect {
		init {
			require(numerator > 0) { "numerator must be positive" }
			require(denominator > 0) { "denominator must be positive" }
			require(numerator <= denominator) { "numerator must not exceed denominator" }
		}
	}

	/**
	 * 造成伤害后按目标实际损失 HP 的比例伤害使用者。
	 *
	 * `numerator / denominator` 表达反作用比例，例如 1/3 表示使用者承受目标本次实际损失 HP 的三分之一。
	 * 该效果只在目标实际损失 HP 后触发；属性免疫、保护、未命中或目标剩余 HP 小于公式伤害时，调用方必须以
	 * 最终实际 HP 损失作为基数。现代公开实现对这类反作用伤害采用四舍五入到最近整数、最少 1 点的口径，
	 * 因此该效果和回复类效果分开建模，避免复用向下取整的回复计算。
	 */
	data class RecoilByDamageDealt(
		val numerator: Int,
		val denominator: Int,
	) : BattleSkillHpEffect {
		init {
			require(numerator > 0) { "numerator must be positive" }
			require(denominator > 0) { "denominator must be positive" }
			require(numerator <= denominator) { "numerator must not exceed denominator" }
		}
	}

	/**
	 * 技能成功后按使用者最大 HP 比例回复。
	 *
	 * 该效果用于表达自我再生、生蛋、羽栖等固定 1/2 最大 HP 回复的稳定规则。
	 */
	data class SelfHealMaxHpFraction(
		val numerator: Int,
		val denominator: Int,
	) : BattleSkillHpEffect {
		init {
			require(numerator > 0) { "numerator must be positive" }
			require(denominator > 0) { "denominator must be positive" }
			require(numerator <= denominator) { "numerator must not exceed denominator" }
		}
	}

	/**
	 * 技能成功后按当前天气选择最大 HP 回复比例。
	 *
	 * 该效果用于表达晨光、光合作用、月光这类现代规则：无相关天气时使用默认比例，晴天等强化天气下改用更高比例，
	 * 雨天、沙暴、雪天等削弱天气下改用更低比例。天气持续、强天气和封锁回复不在这里处理；该对象只负责把
	 * “当前天气 -> 回复比例”变成可复盘的规则快照。
	 */
	data class SelfHealMaxHpByWeather(
		val defaultFraction: HpFraction,
		val weatherFractions: Map<BattleWeather, HpFraction>,
	) : BattleSkillHpEffect {
		init {
			require(weatherFractions.isNotEmpty()) { "weatherFractions must not be empty" }
			require(BattleWeather.NONE !in weatherFractions.keys) { "weather-specific healing cannot target NONE" }
		}
	}

	/**
	 * 技能成功后按实际目标最大 HP 比例回复目标。
	 *
	 * 该效果用于表达治愈波动这类“选择一个目标并回复目标 HP”的稳定规则。回复对象不是技能使用者，而是经过目标
	 * 选择、重定向、保护、命中和免疫 gate 后的实际目标；这样双打重定向或未来新增目标改写规则时，HP 写入仍然
	 * 落在真正被技能影响的成员身上。
	 */
	data class TargetHealMaxHpFraction(
		val numerator: Int,
		val denominator: Int,
	) : BattleSkillHpEffect {
		init {
			require(numerator > 0) { "numerator must be positive" }
			require(denominator > 0) { "denominator must be positive" }
			require(numerator <= denominator) { "numerator must not exceed denominator" }
		}
	}

	/**
	 * 技能成功后按当前场地选择目标最大 HP 回复比例。
	 *
	 * 该效果用于表达“普通场地下回复目标 1/2，特定场地下回复更高比例”的目标治疗。它与
	 * [SelfHealMaxHpByWeather] 分开建模，是因为回复对象、环境维度和未来失败条件都不同；复用一个“通用回复”
	 * 字段反而会让调用方必须靠 targetScope 猜测写入对象。
	 */
	data class TargetHealMaxHpByTerrain(
		val defaultFraction: HpFraction,
		val terrainFractions: Map<BattleTerrain, HpFraction>,
	) : BattleSkillHpEffect {
		init {
			require(terrainFractions.isNotEmpty()) { "terrainFractions must not be empty" }
			require(BattleTerrain.NONE !in terrainFractions.keys) { "terrain-specific healing cannot target NONE" }
		}
	}

	/**
	 * 技能成功后支付最大 HP 的固定比例建立替身。
	 *
	 * 该效果用于表达现代主系列中的替身核心动作：使用者必须拥有多于费用的当前 HP，且当前没有替身；
	 * 成功后本体扣除费用，替身获得同等 HP。替身随后由状态机在伤害和状态附加阶段读取，吸收来自对手的普通伤害，
	 * 并阻止多数主要异常状态和临时状态。接棒传递、穿透替身、束缚和其它复杂例外会用后续显式规则继续扩展。
	 */
	data class CreateSubstitute(
		val numerator: Int,
		val denominator: Int,
	) : BattleSkillHpEffect {
		init {
			require(numerator > 0) { "numerator must be positive" }
			require(denominator > 0) { "denominator must be positive" }
			require(numerator <= denominator) { "numerator must not exceed denominator" }
		}
	}
}
