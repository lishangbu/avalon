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
 * 技能在指定天气下的威力倍率规则。
 *
 * 普通伤害公式在读取技能基础威力后，会先应用天气对技能本身的威力调整，再进入最终伤害计算。
 * 这张表只维护“技能规则 + 天气规则 => 威力倍率”的一对多资料，不混入属性变化、蓄力回合等其它规则。
 *
 * `powerMultiplier` 使用正数倍率表达，0.5 表示威力减半，2.0 表示威力翻倍。服务层会限制倍率范围，
 * 并禁止把“无天气”配置成倍率项，保持运行时映射与引擎模型一致。
 */
@Entity
@Table(name = "battle_skill_weather_power_modifier")
interface BattleSkillWeatherPowerModifier {
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	@JsonConverter(LongToStringConverter::class)
	val id: Long

	@Key
	val skillRuleId: Long

	@Key
	val weatherRuleId: Long

	val powerMultiplier: Double
	val enabled: Boolean
	val sortOrder: Int
}
