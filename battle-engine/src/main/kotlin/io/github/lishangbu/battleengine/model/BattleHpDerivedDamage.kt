package io.github.lishangbu.battleengine.model

/**
 * 技能命中后从战斗双方当前 HP 推导直接伤害的规则。
 *
 * HP 派生伤害和固定伤害、比例伤害一样不进入普通伤害公式：它不读取技能威力、攻防能力值、能力阶级、
 * 属性一致加成、属性克制倍率、击中要害、天气、场地、道具或特性伤害倍率，也不会消费伤害随机数。
 * 目标仍然需要先通过保护、命中、属性免疫、特性吸收和替身等前置流程；属性相性为 0 时不会造成 HP 变化。
 *
 * 该模型只表达现代公开规则中以当前 HP 为输入的直接伤害族：
 * - [TargetCurrentHpMinusUserCurrentHp]：造成目标当前 HP 与使用者当前 HP 的差值伤害，差值不大于 0 时失败。
 * - [UserCurrentHpAndUserFaints]：造成使用者当前 HP 等量伤害，并在命中后让使用者自身倒下。
 *
 * 反击、镜面反射、金属爆炸等依赖“上一段受到的伤害来源和类别”的规则需要额外记录战斗状态，会用后续专门
 * 模型接入，避免把跨行动记忆混进这一类只读取当前 HP 快照的规则。
 */
sealed interface BattleHpDerivedDamage {
	/**
	 * 造成“目标当前 HP - 使用者当前 HP”的直接伤害。
	 *
	 * 该规则在命中结算时读取双方当前 HP。如果目标当前 HP 不高于使用者当前 HP，技能不会进入普通伤害公式，
	 * 而是产生技能失败事件；如果差值为正，则按该差值扣减目标或替身 HP。最终实际 HP 损失仍会被目标当前 HP
	 * 或替身剩余 HP 夹取，不会产生负 HP。
	 */
	object TargetCurrentHpMinusUserCurrentHp : BattleHpDerivedDamage

	/**
	 * 造成使用者当前 HP 等量的直接伤害，并让使用者在命中后倒下。
	 *
	 * 该规则在命中结算时读取使用者当前 HP 作为伤害数值。目标或替身受到伤害后，使用者会以技能自身代价的
	 * 形式损失全部剩余 HP；该代价不是按造成伤害反弹的反作用伤害，也不受反作用伤害免疫影响。
	 */
	object UserCurrentHpAndUserFaints : BattleHpDerivedDamage
}
