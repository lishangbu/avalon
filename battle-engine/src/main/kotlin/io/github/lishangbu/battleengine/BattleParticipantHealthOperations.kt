package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleParticipant

/*
 * `BattleParticipant` 的 HP 与替身运行态操作。
 *
 * 这一组函数只负责成员是否仍可战斗、HP 增减，以及替身 HP 的建立和扣减。它们不直接追加战斗事件，
 * 也不判断命中、免疫或技能成功与否；调用方应在上层流程里把这些状态变化转换成 replay 可见事件。
 */
/**
 * 判断成员是否仍可继续战斗。
 */
fun BattleParticipant.canBattle(): Boolean = currentHp > 0

/**
 * 按伤害值扣除 HP，并把结果夹取到 0。
 */
fun BattleParticipant.receiveDamage(amount: Int): BattleParticipant {
	require(amount >= 0) { "damage amount must not be negative" }
	return copy(currentHp = (currentHp - amount).coerceAtLeast(0))
}

/**
 * 回复 HP，并把结果夹取到最大 HP。
 */
fun BattleParticipant.heal(amount: Int): BattleParticipant {
	require(amount >= 0) { "heal amount must not be negative" }
	return copy(currentHp = (currentHp + amount).coerceAtMost(maxHp))
}

/**
 * 判断成员当前是否拥有替身。
 */
fun BattleParticipant.hasSubstitute(): Boolean = substituteHp > 0

/**
 * 支付 HP 并建立替身。
 *
 * `hpCost` 同时也是替身初始 HP。调用方必须先判断成员没有既有替身、仍可战斗且当前 HP 高于费用；
 * 这里保留 require 作为状态机不变量保护，避免资料层或测试误把失败的替身创建写入运行态。
 */
fun BattleParticipant.startSubstitute(hpCost: Int): BattleParticipant {
	require(hpCost > 0) { "substitute HP cost must be positive" }
	require(!hasSubstitute()) { "participant already has a substitute" }
	require(currentHp > hpCost) { "participant must have more HP than substitute cost" }
	return copy(
		currentHp = currentHp - hpCost,
		substituteHp = hpCost,
	)
}

/**
 * 让替身承受伤害。
 *
 * 返回值只修改替身剩余 HP，不修改成员本体 HP。调用方负责根据返回值追加“替身受击”或“替身破裂”等事件，
 * 并决定本次技能后续效果是否仍被替身阻止。
 */
fun BattleParticipant.damageSubstitute(amount: Int): BattleParticipant {
	require(amount >= 0) { "substitute damage amount must not be negative" }
	return if (amount == 0 || substituteHp == 0) {
		this
	} else {
		copy(substituteHp = (substituteHp - amount).coerceAtLeast(0))
	}
}
