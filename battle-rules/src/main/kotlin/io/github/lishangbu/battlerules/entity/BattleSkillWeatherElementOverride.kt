package io.github.lishangbu.battlerules.entity

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Key
import org.babyfish.jimmer.sql.Table

/**
 * 技能在指定天气下的属性覆盖规则。
 *
 * 有些技能会在当前天气存在时把本次技能结算属性替换为天气对应属性。该表只维护“技能规则 + 天气规则 =>
 * 目标属性”的三范式关系，不复制技能名称、天气名称或属性名称；展示文案由管理端通过引用资料解析。
 *
 * 运行时读取该资料后会把属性覆盖写入技能槽，并统一影响属性一致加成、属性克制、天气火水倍率、属性吸收特性
 * 和抗性道具等所有依赖技能属性的规则。服务层禁止引用“无天气”，因为无天气时应直接使用技能基础属性。
 */
@Entity
@Table(name = "battle_skill_weather_element_override")
interface BattleSkillWeatherElementOverride {
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	val id: Long

	@Key
	val skillRuleId: Long

	@Key
	val weatherRuleId: Long

	val targetElementId: Long
	val enabled: Boolean
	val sortOrder: Int
}
