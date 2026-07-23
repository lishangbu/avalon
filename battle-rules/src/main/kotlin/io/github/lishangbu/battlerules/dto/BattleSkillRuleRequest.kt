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
	@field:Schema(description = "最小连续命中段数，单段技能为 1。", example = "1")
	var minHits: Int = 1,
	@field:Schema(description = "最大连续命中段数，单段技能为 1。", example = "1")
	var maxHits: Int = 1,
	@field:Schema(description = "基础击中要害等级，0 为普通技能，3 及以上按现代规则视为必定要害。", example = "0")
	var criticalHitStage: Int = 0,
	@field:Schema(description = "是否接触目标。", example = "false")
	var makesContact: Boolean = false,
	@field:Schema(description = "是否属于风类技能。", example = "false")
	var windBased: Boolean = false,
	@field:Schema(description = "是否属于舞蹈类技能。", example = "false")
	var danceBased: Boolean = false,
	@field:Schema(description = "是否让战斗切换特性的使用者回到防御形态。", example = "false")
	var returnsUserToDefensiveForm: Boolean = false,
	@field:Schema(description = "是否会被保护类行动阻挡。", example = "true")
	var affectedByProtect: Boolean = true,
	@field:Schema(description = "是否在本回合为使用者建立保护屏障。", example = "false")
	var protectsUser: Boolean = false,
	@field:Schema(description = "是否在本回合让使用者承受致命技能伤害时至少保留 1 HP。", example = "false")
	var enduresFatalDamage: Boolean = false,
	@field:Schema(description = "是否允许冰冻中的使用者发动并在行动前解除自身冰冻。", example = "false")
	var thawsUserBeforeMove: Boolean = false,
	@field:Schema(description = "是否属于会被青草场地削弱的地面震动类技能。", example = "false")
	var weakenedByGrassyTerrain: Boolean = false,
	@field:Schema(description = "首次使用时是否需要先蓄力，下一次行动才释放技能效果。", example = "false")
	var chargesBeforeUse: Boolean = false,
	@field:Schema(description = "成功造成实际伤害后是否让使用者下一次行动前休整。", example = "false")
	var rechargesAfterUse: Boolean = false,
	@field:Schema(description = "是否属于声音类效果。", example = "false")
	var soundBased: Boolean = false,
	@field:Schema(description = "是否属于粉末或孢子类效果。", example = "false")
	var powderBased: Boolean = false,
	@field:Schema(description = "是否属于拳击类效果。", example = "false")
	var punchBased: Boolean = false,
	@field:Schema(description = "是否属于切割类效果。", example = "false")
	var slicingBased: Boolean = false,
	var projectileBased: Boolean = false,
	var pulseBased: Boolean = false,
	var biteBased: Boolean = false,
	@field:Schema(description = "锁定连续使用该技能的最小总回合数，包含首次使用回合。", example = "1")
	var lockMoveTurnsMin: Int = 1,
	@field:Schema(description = "锁定连续使用该技能的最大总回合数，包含首次使用回合。", example = "1")
	var lockMoveTurnsMax: Int = 1,
	@field:Schema(description = "锁招结束后使用者是否进入混乱状态。", example = "false")
	var confusesUserAfterLock: Boolean = false,
	@field:Schema(description = "成功命中后是否强制目标侧换入后备成员。", example = "false")
	var forceTargetSwitch: Boolean = false,
	@field:Schema(description = "技能规则说明。", nullable = true)
	var description: String? = null,
	@field:Schema(description = "是否启用。", example = "true")
	var enabled: Boolean = true,
	@field:Schema(description = "展示排序。", example = "10")
	var sortOrder: Int = 0,
)
