package io.github.lishangbu.gamedata.entity

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table

/**
 * 精灵技能学习持久化实体。
 *
 * 对应 `game_creature_skill_learn` 表，仅承载一条精灵技能学习记录及其标量属性。
 * 关联项保留为独立 ID，避免把其他功能的实体聚合进当前资料对象；新增记录的主键统一由 CosId 生成。
 */
@Entity
@Table(name = "game_creature_skill_learn")
interface GameCreatureSkillLearns {
	/**
	 * 精灵技能学习记录的唯一标识。
	 *
	 * 新增时由 [CosIdLongUserIdGenerator] 生成；对外 JSON 通过 [LongToStringConverter] 输出为字符串，
	 * 避免 JavaScript 数值精度丢失，同时在 Kotlin 和数据库中保持 Long 类型。
	 */
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	@JsonConverter(LongToStringConverter::class)
	val id: Long

	/**
	 * 精灵技能学习引用的精灵 ID。
	 *
	 * 对应数据库 `creature_id` 列，写入时必须提供；关联标识以 Long 标量保存，不在该实体中聚合关联对象。
	 */
	val creatureId: Long

	/**
	 * 精灵技能学习引用的技能 ID。
	 *
	 * 对应数据库 `skill_id` 列，写入时必须提供；关联标识以 Long 标量保存，不在该实体中聚合关联对象。
	 */
	val skillId: Long

	/**
	 * 精灵技能学习引用的学习方式 ID。
	 *
	 * 对应数据库 `learn_method_id` 列，写入时必须提供；关联标识以 Long 标量保存，不在该实体中聚合关联对象。
	 */
	val learnMethodId: Long

	/**
	 * 精灵技能学习的习得等级。
	 *
	 * 对应数据库 `level_learned_at` 列，写入时必须提供。
	 */
	val levelLearnedAt: Int
}
