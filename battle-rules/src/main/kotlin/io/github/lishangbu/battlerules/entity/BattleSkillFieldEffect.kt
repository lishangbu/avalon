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
 * 技能建立场上持续效果的规则定义。
 *
 * 这张表连接“技能规则”和“场上效果规则”：例如某个变化技能命中后，在使用者一侧建立物理伤害减免屏障。
 * 基础效果事实仍保存在 `battle_field_rule`，这里只保存该技能如何引用该效果、作用在哪一侧、什么时候结算、
 * 触发概率，以及是否要求某个天气已经存在。
 *
 * 当前运行时把 SIDE 作用域下的防守屏障和速度修正映射进引擎；其它场上效果可以先在资料层独立维护，等对应
 * 引擎模型完成后再通过显式枚举映射接入。
 */
@Entity
@Table(name = "battle_skill_field_effect")
interface BattleSkillFieldEffect {
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	@JsonConverter(LongToStringConverter::class)
	val id: Long

	@Key
	val skillRuleId: Long

	@Key
	val fieldRuleId: Long

	@Key
	val targetSide: String

	@Key
	val effectTiming: String

	val requiredWeatherRuleId: Long?
	val chancePercent: Int
	val enabled: Boolean
	val sortOrder: Int
}
