package io.github.lishangbu.battleengine.model

/**
 * 技能命中后使用固定伤害口径扣减目标 HP 的规则。
 *
 * 固定伤害属于“命中后的直接技能伤害”，但不进入普通物理/特殊伤害公式：它不读取技能威力、攻防能力值、
 * 能力阶级、属性一致加成、属性克制倍率、击中要害、天气、场地、道具或特性伤害倍率，也不会消费伤害随机数。
 * 目标仍然需要先通过保护、命中、属性免疫、特性吸收和替身等前置流程；属性相性为 0 时仍然不会造成 HP 变化。
 *
 * 该模型只表达现代公开规则中最稳定的固定伤害族：
 * - [FixedAmount]：固定数值，例如固定造成 20 或 40 点 HP 损失。
 * - [UserLevel]：按使用者当前等级造成同等 HP 损失。
 *
 * 比例扣血和按双方当前 HP 推导的直接伤害使用独立模型承载；反击、随机固定伤害等规则会继续用后续专门模型
 * 接入，避免把不同取整、失败条件和事件顺序混在同一个字段里。
 */
sealed interface BattleFixedDamage {
	/**
	 * 固定扣减目标 HP 的数值。
	 *
	 * `amount` 是未夹取的固定伤害；状态机会在写入目标 HP 时按目标当前 HP 夹取实际 HP 损失，并继续触发
	 * 伤害后流程。该规则不因为目标弱点或抗性改变数值。
	 */
	data class FixedAmount(
		val amount: Int,
	) : BattleFixedDamage {
		init {
			require(amount > 0) { "amount must be positive" }
		}
	}

	/**
	 * 按使用者等级造成固定伤害。
	 *
	 * 状态机会在命中结算时读取使用者运行态等级，因此等级统一赛制、临时构造的测试用例或自定义格式都能自然
	 * 影响该数值。该规则不读取目标等级，也不读取技能威力。
	 */
	object UserLevel : BattleFixedDamage
}
