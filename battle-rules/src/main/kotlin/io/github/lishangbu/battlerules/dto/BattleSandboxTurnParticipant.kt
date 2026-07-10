package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/**
 * 回合响应中用于管理页面展示的成员运行态摘要。
 *
 * 该响应只暴露 HP、主要异常、能力阶级和技能 PP 等常用排障字段。锁招、蓄力、替身和陷阱等连续结算状态由
 * [BattleSandboxStateSnapshot.Participant] 保存，调用方不能使用本摘要恢复下一回合。
 */
@Schema(name = "BattleSandboxTurnParticipant", description = "成员运行态摘要。")
@Immutable
interface BattleSandboxTurnParticipant {
	@get:Schema(description = "战斗内成员 ID。", example = "side-a-1")
	val actorId: String
	@get:Schema(description = "精灵资料 ID。", type = "string", example = "1")
	@JsonConverter(LongToStringConverter::class)
	val creatureId: Long
	@get:Schema(description = "是否当前上场。", example = "true")
	val active: Boolean
	@get:Schema(description = "等级。", example = "50")
	val level: Int
	@get:Schema(description = "当前 HP。", example = "100")
	val currentHp: Int
	@get:Schema(description = "最大 HP。", example = "120")
	val maxHp: Int
	@get:Schema(description = "主要异常状态；无异常时为空。", nullable = true, example = "BURN")
	val majorStatus: String?
	@get:Schema(description = "能力阶级变化。")
	val statStages: Map<String, Int>
	@get:Schema(description = "技能槽运行态。")
	val skillSlots: List<BattleSandboxTurnSkillSlot>
}
