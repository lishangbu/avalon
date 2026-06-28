package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗技能规则维护响应。
 */
@Schema(description = "战斗技能规则维护响应。")
data class BattleSkillRuleResponse(
	@field:Schema(description = "技能规则主键 ID。", example = "1")
	val id: Long,
	@field:Schema(description = "技能 ID，引用基础游戏资料。", example = "85")
	val skillId: Long,
	@field:Schema(description = "技能主效果策略编码。", example = "standard-damage-with-status")
	val effectPolicy: String,
	@field:Schema(description = "目标选择策略编码。", example = "selected-target")
	val targetPolicy: String,
	@field:Schema(description = "命中判定策略编码。", example = "standard-hit")
	val hitPolicy: String,
	@field:Schema(description = "伤害策略编码。", example = "standard-damage")
	val damagePolicy: String,
	@field:Schema(description = "是否接触目标。", example = "false")
	val makesContact: Boolean,
	@field:Schema(description = "是否会被保护类行动阻挡。", example = "true")
	val affectedByProtect: Boolean,
	@field:Schema(description = "是否属于声音类效果。", example = "false")
	val soundBased: Boolean,
	@field:Schema(description = "是否属于粉末或孢子类效果。", example = "false")
	val powderBased: Boolean,
	@field:Schema(description = "是否属于拳击类效果。", example = "false")
	val punchBased: Boolean,
	@field:Schema(description = "是否属于切割类效果。", example = "false")
	val slicingBased: Boolean,
	@field:Schema(description = "技能规则说明。", nullable = true)
	val description: String?,
	@field:Schema(description = "是否启用。", example = "true")
	val enabled: Boolean,
	@field:Schema(description = "展示排序。", example = "10")
	val sortOrder: Int,
)
