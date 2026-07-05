package io.github.lishangbu.battlerules.entity

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Key
import org.babyfish.jimmer.sql.Table

/**
 * 战斗技能规则定义。
 *
 * 这张表把资料库中的技能映射到战斗引擎可执行的策略组合：目标选择、命中判定、伤害公式和常用标签。
 * 它不复制技能威力、属性、PP 等基础资料，那些事实仍以 `game_skill` 为准；这里仅保存现代战斗规则需要额外解释的行为。
 *
 * 后续引擎读取规则时会先通过 `skillId` 定位此记录，再按各类 policy 字段选择实现类。这样可以让管理端维护
 * “某个技能如何执行”，而不是把 Kotlin 类名、条件分支或原始 JSON 泄漏进数据库。
 */
@Entity
@Table(name = "battle_skill_rule")
interface BattleSkillRule {
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	val id: Long

	@Key
	val skillId: Long

	val effectPolicy: String
	val targetPolicy: String
	val hitPolicy: String
	val damagePolicy: String
	val minHits: Int
	val maxHits: Int
	val criticalHitStage: Int
	val makesContact: Boolean
	val affectedByProtect: Boolean
	val protectsUser: Boolean
	val enduresFatalDamage: Boolean
	val thawsUserBeforeMove: Boolean
	val weakenedByGrassyTerrain: Boolean
	val chargesBeforeUse: Boolean
	val rechargesAfterUse: Boolean
	val soundBased: Boolean
	val powderBased: Boolean
	val punchBased: Boolean
	val slicingBased: Boolean
	val lockMoveTurnsMin: Int
	val lockMoveTurnsMax: Int
	val confusesUserAfterLock: Boolean
	val forceTargetSwitch: Boolean
	val description: String?
	val enabled: Boolean
	val sortOrder: Int
}
