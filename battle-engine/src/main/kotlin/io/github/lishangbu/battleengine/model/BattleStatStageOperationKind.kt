package io.github.lishangbu.battleengine.model

/**
 * 能力阶级操作类型。
 *
 * `CLEAR` 把目标指定能力阶级归零，`COPY` 把来源的当前阶级复制给目标，`SWAP` 交换来源和目标当前阶级，
 * `INVERT` 将目标当前阶级取反。所有操作都只改变战斗运行态，不改变基础能力值。
 */
enum class BattleStatStageOperationKind {
	CLEAR,
	COPY,
	SWAP,
	INVERT,
}

