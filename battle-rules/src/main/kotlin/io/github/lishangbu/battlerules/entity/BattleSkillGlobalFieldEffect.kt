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
 * 技能建立全场持续效果的规则定义。
 *
 * 这张表连接“技能规则”和“全场效果规则”：例如某个变化技能命中后建立戏法空间，从而改变全场速度排序。
 * 它只保存 FIELD 作用域效果，不包含 `targetSide`，避免把全场空间类规则塞进一侧效果资源的语义里。
 *
 * 运行时不会直接解释这里的自由文本字段，而是由 `BattleRuntimeSnapshotService` 将 `battle_field_rule.effect_policy`
 * 显式映射为 battle-engine 的强类型模型。
 */
@Entity
@Table(name = "battle_skill_global_field_effect")
interface BattleSkillGlobalFieldEffect {
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	@JsonConverter(LongToStringConverter::class)
	val id: Long

	@Key
	val skillRuleId: Long

	@Key
	val fieldRuleId: Long

	@Key
	val effectTiming: String

	val requiredWeatherRuleId: Long?
	val chancePercent: Int
	val enabled: Boolean
	val sortOrder: Int
}
