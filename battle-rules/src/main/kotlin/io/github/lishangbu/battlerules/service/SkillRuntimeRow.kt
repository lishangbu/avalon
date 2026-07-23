package io.github.lishangbu.battlerules.service

/**
 * 技能基础资料和启用战斗规则合并后的运行时中间行。
 *
 * 该类型只在 `battle-rules` 查询适配层中使用，用于先收敛可空数据库字段，再装配纯引擎的技能槽；它不作为
 * Controller 响应，也不承担持久化职责。
 */
internal data class SkillRuntimeRow(
	val skillId: Long,
	val name: String,
	val elementId: Long,
	val damageClassCode: String,
	val power: Int?,
	val accuracy: Int?,
	val pp: Int,
	val priority: Int,
	val ruleId: Long?,
	val effectPolicy: String?,
	val targetPolicy: String?,
	val hitPolicy: String?,
	val damagePolicy: String?,
	val minHits: Int?,
	val maxHits: Int?,
	val criticalHitStage: Int?,
	val makesContact: Boolean?,
	val windBased: Boolean?,
	val danceBased: Boolean?,
	val affectedByProtect: Boolean?,
	val protectsUser: Boolean?,
	val enduresFatalDamage: Boolean?,
	val thawsUserBeforeMove: Boolean?,
	val soundBased: Boolean?,
	val powderBased: Boolean?,
	val punchBased: Boolean?,
	val slicingBased: Boolean?,
	val projectileBased: Boolean?,
	val pulseBased: Boolean?,
	val biteBased: Boolean?,
	val weakenedByGrassyTerrain: Boolean?,
	val chargesBeforeUse: Boolean?,
	val rechargesAfterUse: Boolean?,
	val lockMoveTurnsMin: Int?,
	val lockMoveTurnsMax: Int?,
	val confusesUserAfterLock: Boolean?,
	val forceTargetSwitch: Boolean?,
)
