package io.github.lishangbu.battlerules.service

/**
 * 成员资料进入战斗引擎前的运行时能力画像。
 *
 * 该画像已经把基础能力、等级、默认个体值、默认努力值和中性性格折算成最终战斗数值，
 * 并携带资料源中的当前体重，因此 battle-engine 不需要知道这些数值来自哪些资料表，也不需要在结算阶段回读数据库。
 * [elementIds] 使用资料侧属性 ID，和 [io.github.lishangbu.battleengine.model.BattleRuleSnapshot.elementIds]
 * 共同保证属性判断只依赖一次启动快照。
 */
data class BattleCreatureRuntimeProfile(
	val maxHp: Int,
	val attack: Int,
	val defense: Int,
	val specialAttack: Int,
	val specialDefense: Int,
	val speed: Int,
	val weight: Int,
	val elementIds: Set<Long>,
)
