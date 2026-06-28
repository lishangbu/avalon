package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗技能规则维护请求。
 */
@Schema(description = "战斗技能规则维护请求。")
data class BattleSkillRuleRequest(
	@field:Schema(description = "技能 ID，引用基础游戏资料。", example = "85")
	var skillId: Long = 0,
	@field:Schema(description = "技能主效果策略编码。", example = "standard-damage-with-status")
	var effectPolicy: String = "",
	@field:Schema(description = "目标选择策略编码。", example = "selected-target")
	var targetPolicy: String = "",
	@field:Schema(description = "命中判定策略编码。", example = "standard-hit")
	var hitPolicy: String = "",
	@field:Schema(description = "伤害策略编码。", example = "standard-damage")
	var damagePolicy: String = "",
	@field:Schema(description = "是否接触目标。", example = "false")
	var makesContact: Boolean = false,
	@field:Schema(description = "是否会被保护类行动阻挡。", example = "true")
	var affectedByProtect: Boolean = true,
	@field:Schema(description = "是否属于声音类效果。", example = "false")
	var soundBased: Boolean = false,
	@field:Schema(description = "是否属于粉末或孢子类效果。", example = "false")
	var powderBased: Boolean = false,
	@field:Schema(description = "是否属于拳击类效果。", example = "false")
	var punchBased: Boolean = false,
	@field:Schema(description = "是否属于切割类效果。", example = "false")
	var slicingBased: Boolean = false,
	@field:Schema(description = "技能规则说明。", nullable = true)
	var description: String? = null,
	@field:Schema(description = "是否启用。", example = "true")
	var enabled: Boolean = true,
	@field:Schema(description = "展示排序。", example = "10")
	var sortOrder: Int = 0,
)
