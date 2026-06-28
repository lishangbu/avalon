package io.github.lishangbu.battleengine.model

/**
 * 技能伤害类别。
 *
 * `PHYSICAL` 使用攻击和防御计算伤害，`SPECIAL` 使用特攻和特防计算伤害，
 * `STATUS` 表示不直接走普通伤害公式。第一阶段引擎只执行物理和特殊伤害技能；
 * 变化技能会产生使用事件但不会造成伤害，后续由状态、天气、能力阶级等效果处理器扩展。
 */
enum class BattleDamageClass {
	PHYSICAL,
	SPECIAL,
	STATUS,
}
