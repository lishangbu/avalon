package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/**
 * 回合响应中用于管理页面展示的技能槽运行态摘要。
 *
 * 名称和 PP 用于观察技能消耗；技能效果、命中率、威力和目标规则由战斗规则快照负责，不在该摘要中重复返回。
 */
@Schema(name = "BattleSandboxTurnSkillSlot", description = "技能槽运行态。")
@Immutable
interface BattleSandboxTurnSkillSlot {
	@get:Schema(description = "技能资料 ID。", type = "string", example = "33")
	@JsonConverter(LongToStringConverter::class)
	val skillId: Long
	@get:Schema(description = "技能名称。", example = "撞击")
	val name: String
	@get:Schema(description = "剩余 PP。", example = "34")
	val remainingPp: Int
	@get:Schema(description = "最大 PP。", example = "35")
	val maxPp: Int
}
