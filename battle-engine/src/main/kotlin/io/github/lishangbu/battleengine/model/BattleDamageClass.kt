package io.github.lishangbu.battleengine.model

/**
 * 技能伤害类别。
 *
 * `PHYSICAL` 使用攻击和防御计算伤害，`SPECIAL` 使用特攻和特防计算伤害，
 * `STATUS` 表示不直接走普通伤害公式。变化技能会产生使用事件，并由状态、天气、能力阶级、HP 等
 * 显式效果处理器继续结算。
 */
enum class BattleDamageClass {
	PHYSICAL,
	SPECIAL,
	STATUS,
}
