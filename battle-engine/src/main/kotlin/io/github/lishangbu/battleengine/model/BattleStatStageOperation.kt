package io.github.lishangbu.battleengine.model

/**
 * 技能命中后执行的能力阶级结构化操作。
 *
 * 这类规则不同于普通 `stageDelta` 加减：清除、复制、交换和取反都需要读取当前战斗成员的已有阶级，再把结果写回。
 * 每条操作只针对一个能力项，资料层可以为同一技能配置多条记录来表达“全部能力项”或“攻击组/防御组”。
 */
data class BattleStatStageOperation(
	val kind: BattleStatStageOperationKind,
	val stat: BattleStat,
	val target: BattleStatStageOperationTarget,
	val source: BattleStatStageOperationTarget? = null,
	val chancePercent: Int,
) {
	init {
		require(chancePercent in 0..100) { "chancePercent must be between 0 and 100" }
		when (kind) {
			BattleStatStageOperationKind.CLEAR,
			BattleStatStageOperationKind.INVERT,
			-> require(source == null) { "$kind does not use source target" }
			BattleStatStageOperationKind.COPY,
			BattleStatStageOperationKind.SWAP,
			-> require(source != null) { "$kind requires source target" }
		}
		require(target != BattleStatStageOperationTarget.ALL_ACTIVE || kind == BattleStatStageOperationKind.CLEAR) {
			"ALL_ACTIVE target is only supported by CLEAR"
		}
		require(source != BattleStatStageOperationTarget.ALL_ACTIVE) {
			"ALL_ACTIVE cannot be used as a source target"
		}
	}
}

