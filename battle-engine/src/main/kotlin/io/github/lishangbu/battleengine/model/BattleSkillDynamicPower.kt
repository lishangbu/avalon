package io.github.lishangbu.battleengine.model

/**
 * 技能进入普通伤害公式前的动态基础威力规则。
 *
 * 这类规则和 [BattleSkillPowerMultiplier] 分开建模：倍率类规则会拿技能表中的固定 `power` 继续相乘，
 * 而动态威力类规则会先根据当前战斗快照重新计算“本次公式使用的基础威力”。例如辅助力量、嚣张会读取使用者
 * 正向能力阶级总和；惩罚会读取目标正向能力阶级总和，并且拥有现代公开规则中常见的上限。
 *
 * 模型不保存具体技能名称，只保存公式参数和读取哪一方。这样运行时可以用同一条分支覆盖同类技能，也不会把
 * 资料层的英文 code 泄漏进伤害计算器。`source` 只允许 [BattleEffectTarget.USER] 或 [BattleEffectTarget.TARGET]，
 * 因为动态威力始终发生在一次逐目标伤害结算中：范围技能会被拆成多个实际目标后分别计算。
 */
sealed interface BattleSkillDynamicPower {
	/**
	 * 按某一方所有正向能力阶级总和计算基础威力。
	 *
	 * `basePower + powerPerPositiveStage * positiveStageSum` 是进入普通伤害公式的基础威力；负向阶级和 0 阶级
	 * 不参与累计，命中率与闪避率的正向阶级也会被计入，因为这类公开规则描述的是“所有能力提升”。`maxPower`
	 * 为空时不设上限；非空时在返回前截断，适合惩罚这类有 200 威力上限的技能。
	 */
	data class PositiveStatStageSum(
		val source: BattleEffectTarget,
		val basePower: Int,
		val powerPerPositiveStage: Int,
		val maxPower: Int? = null,
	) : BattleSkillDynamicPower {
		init {
			require(basePower > 0) { "basePower must be positive" }
			require(powerPerPositiveStage > 0) { "powerPerPositiveStage must be positive" }
			require(maxPower == null || maxPower >= basePower) { "maxPower must be null or at least basePower" }
		}
	}
}
