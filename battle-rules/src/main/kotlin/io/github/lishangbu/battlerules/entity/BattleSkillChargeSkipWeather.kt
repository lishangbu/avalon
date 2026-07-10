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
 * 技能在指定天气下跳过蓄力回合的规则。
 *
 * 现代规则中，只有部分蓄力技能会因为指定天气直接发动；其它蓄力技能仍必须等待下一次行动。这里使用独立
 * 关联表保存“技能规则 + 天气规则”的允许项，避免在 `chargesBeforeUse` 这个通用布尔字段里隐式假设所有蓄力
 * 技能都受同一种天气影响。
 *
 * 本表只表达是否跳过蓄力，不表达威力、命中或属性变化；这些行为由对应的天气命中覆盖、天气威力倍率等表维护。
 */
@Entity
@Table(name = "battle_skill_charge_skip_weather")
interface BattleSkillChargeSkipWeather {
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	@JsonConverter(LongToStringConverter::class)
	val id: Long

	@Key
	val skillRuleId: Long

	@Key
	val weatherRuleId: Long

	val enabled: Boolean
	val sortOrder: Int
}
