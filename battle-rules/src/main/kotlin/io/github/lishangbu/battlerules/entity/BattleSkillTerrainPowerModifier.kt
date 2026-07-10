package io.github.lishangbu.battlerules.entity

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Key
import org.babyfish.jimmer.sql.Table

/**
 * 技能在指定场地下的威力倍率规则。
 *
 * 该实体表达“技能规则 + 场地规则 => 威力倍率”的结构化资料。和天气倍率不同，现代主系列场地效果通常要求
 * 使用者接触地面后才影响技能，因此运行时会把这里的倍率装配到 `groundedPowerMultipliersByTerrain`，并在
 * 伤害公式的威力阶段先判断使用者是否接地。
 *
 * 这张表只保存倍率事实，不保存目标属性覆盖、场地持续回合或状态免疫；场地脉冲一类技能会同时使用本表和
 * [BattleSkillTerrainElementOverride]。这种拆法能让每个事实只有一个来源，避免在描述文本或策略字符串中重复维护。
 */
@Entity
@Table(name = "battle_skill_terrain_power_modifier")
interface BattleSkillTerrainPowerModifier {
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	@JsonConverter(LongToStringConverter::class)
	val id: Long

	@Key
	val skillRuleId: Long

	@Key
	val terrainRuleId: Long

	val powerMultiplier: Double
	val enabled: Boolean
	val sortOrder: Int
}
