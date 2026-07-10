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
 * 技能在指定天气下的命中覆盖规则。
 *
 * 这张表只描述“某个技能规则遇到某种天气时，命中率是否被覆盖”。它不保存基础命中率，也不保存天气本身的
 * 持续回合或伤害效果；这些事实分别属于 `game_skill` 和 `battle_weather_rule`。
 *
 * `accuracyPercent` 为空是有业务含义的：表示该天气下必中，而不是未知值。服务层会限制非空值只能位于
 * 1 到 100 之间，并禁止使用“无天气”规则，避免引擎收到 `BattleWeather.NONE` 的覆盖项。
 */
@Entity
@Table(name = "battle_skill_weather_accuracy_override")
interface BattleSkillWeatherAccuracyOverride {
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	@JsonConverter(LongToStringConverter::class)
	val id: Long

	@Key
	val skillRuleId: Long

	@Key
	val weatherRuleId: Long

	val accuracyPercent: Int?
	val enabled: Boolean
	val sortOrder: Int
}
