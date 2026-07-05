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
	@field:Schema(description = "最小连续命中段数，单段技能为 1。", example = "1")
	val minHits: Int,
	@field:Schema(description = "最大连续命中段数，单段技能为 1。", example = "1")
	val maxHits: Int,
	@field:Schema(description = "基础击中要害等级，0 为普通技能，3 及以上按现代规则视为必定要害。", example = "0")
	val criticalHitStage: Int,
	@field:Schema(description = "是否接触目标。", example = "false")
	val makesContact: Boolean,
	@field:Schema(description = "是否会被保护类行动阻挡。", example = "true")
	val affectedByProtect: Boolean,
	@field:Schema(description = "是否在本回合为使用者建立保护屏障。", example = "false")
	val protectsUser: Boolean,
	@field:Schema(description = "是否在本回合让使用者承受致命技能伤害时至少保留 1 HP。", example = "false")
	val enduresFatalDamage: Boolean,
	@field:Schema(description = "是否允许冰冻中的使用者发动并在行动前解除自身冰冻。", example = "false")
	val thawsUserBeforeMove: Boolean,
	@field:Schema(description = "是否属于会被青草场地削弱的地面震动类技能。", example = "false")
	val weakenedByGrassyTerrain: Boolean,
	@field:Schema(description = "首次使用时是否需要先蓄力，下一次行动才释放技能效果。", example = "false")
	val chargesBeforeUse: Boolean,
	@field:Schema(description = "成功造成实际伤害后是否让使用者下一次行动前休整。", example = "false")
	val rechargesAfterUse: Boolean,
	@field:Schema(description = "是否属于声音类效果。", example = "false")
	val soundBased: Boolean,
	@field:Schema(description = "是否属于粉末或孢子类效果。", example = "false")
	val powderBased: Boolean,
	@field:Schema(description = "是否属于拳击类效果。", example = "false")
	val punchBased: Boolean,
	@field:Schema(description = "是否属于切割类效果。", example = "false")
	val slicingBased: Boolean,
	@field:Schema(description = "锁定连续使用该技能的最小总回合数，包含首次使用回合。", example = "1")
	val lockMoveTurnsMin: Int,
	@field:Schema(description = "锁定连续使用该技能的最大总回合数，包含首次使用回合。", example = "1")
	val lockMoveTurnsMax: Int,
	@field:Schema(description = "锁招结束后使用者是否进入混乱状态。", example = "false")
	val confusesUserAfterLock: Boolean,
	@field:Schema(description = "成功命中后是否强制目标侧换入后备成员。", example = "false")
	val forceTargetSwitch: Boolean,
	@field:Schema(description = "技能规则说明。", nullable = true)
	val description: String?,
	@field:Schema(description = "是否启用。", example = "true")
	val enabled: Boolean,
	@field:Schema(description = "展示排序。", example = "10")
	val sortOrder: Int,
)
