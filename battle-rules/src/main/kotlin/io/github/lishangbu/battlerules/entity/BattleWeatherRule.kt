package io.github.lishangbu.battlerules.entity

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Key
import org.babyfish.jimmer.sql.Table

/**
 * 全场天气规则定义。
 *
 * 天气会影响伤害、回复、持续伤害和部分技能行为。实体只保存现代规则配置和策略编码，
 * 具体计算顺序由战斗引擎的天气规则实现负责。
 */
@Entity
@Table(name = "battle_weather_rule")
interface BattleWeatherRule {
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	val id: Long

	@Key
	val code: String

	val name: String
	val effectPolicy: String
	val defaultDurationTurns: Int?
	val description: String?
	val enabled: Boolean
	val sortOrder: Int
}
