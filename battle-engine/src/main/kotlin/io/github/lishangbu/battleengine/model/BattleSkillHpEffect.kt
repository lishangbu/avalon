package io.github.lishangbu.battleengine.model

/**
 * 技能在命中后直接改变 HP 的结构化效果。
 *
 * 这里刻意不把技能名称或本地化描述写进引擎，而是把资料层的 policy code 转换成可复盘的数值规则。
 * 第一批只覆盖现代主系列中最稳定、最常见的两类 HP 效果：
 * - 造成普通伤害后，使用者按本次实际伤害的一定比例回复。
 * - 变化技能成功后，使用者按自身最大 HP 的一定比例回复。
 *
 * 天气可变回复、吸取回复强化、污泥浆反转、回复封锁、许愿等带有额外状态或环境依赖的规则，会以新的
 * 明确效果继续扩展，而不是把条件藏进自由文本。
 */
sealed interface BattleSkillHpEffect {
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
	 * 技能成功后按使用者最大 HP 比例回复。
	 *
	 * 该效果用于表达自我再生、生蛋、羽栖等固定 1/2 最大 HP 回复的稳定规则。天气影响回复量的技能不复用该
	 * 效果，避免把天气分支硬塞进一个看似简单的固定比例模型里。
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
}
