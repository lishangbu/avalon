package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

/** 将内存 Runtime 状态限制为管理端可观察字段，不暴露引擎内部规则对象。 */
@Schema(description = "管理端可观察的临时 Battle Session 快照。")
data class BattleSessionResponse(
	@field:Schema(description = "服务端生成的 Session Identifier。", example = "550e8400-e29b-41d4-a716-446655440000")
	val sessionId: String,
	@field:Schema(description = "赛制稳定 code。", example = "official-double")
	val formatCode: String,
	@field:Schema(description = "会话生命周期状态。", allowableValues = ["ACTIVE", "COMPLETED", "TERMINATED"])
	val status: String,
	@field:Schema(description = "并发控制 revision。", example = "0")
	val revision: Long,
	@field:Schema(description = "当前已结算回合序号。", example = "0")
	val turnNumber: Int,
	@field:Schema(description = "引擎已确认的战斗结果；未结束时为空。", nullable = true)
	val result: Result?,
	@field:Schema(description = "外部终止事实；非 TERMINATED 状态时为空。", nullable = true)
	val termination: Termination?,
	@field:Schema(description = "会话创建时间。")
	val createdAt: Instant,
	@field:Schema(description = "会话最后更新时间。")
	val updatedAt: Instant,
	@field:Schema(description = "会话进入终态的时间。", nullable = true)
	val endedAt: Instant?,
	@field:Schema(description = "Recent Session 预计淘汰时间。", nullable = true)
	val expiresAt: Instant?,
	@field:Schema(description = "双方当前运行态摘要。")
	val sides: List<Side>,
	@field:Schema(description = "下一回合必须选择的行动及其合法选项。")
	val turnRequirements: List<TurnRequirement>,
) {
	/** 区分引擎自然完成结果与外部终止事实。 */
	@Schema(name = "BattleSessionResult", description = "引擎确认的战斗结果。")
	data class Result(
		@field:Schema(description = "获胜方 sideId；平局时为空。", nullable = true)
		val winningSideId: String?,
		@field:Schema(description = "稳定结果原因。")
		val reason: String,
	)

	/** 保存显式终止的幂等标识与 revision 变化，不伪造战斗胜负。 */
	@Schema(name = "BattleSessionTermination", description = "外部终止事实。")
	data class Termination(
		@field:Schema(description = "终止命令幂等标识。")
		val commandId: String,
		@field:Schema(description = "终止原因。")
		val reason: String,
		@field:Schema(description = "终止前 revision。")
		val revisionBefore: Long,
		@field:Schema(description = "终止后 revision。")
		val revisionAfter: Long,
		@field:Schema(description = "终止时间。")
		val terminatedAt: Instant,
	)

	/** 按固定阵容顺序公开一侧的上场标识与成员状态。 */
	@Schema(name = "BattleSessionSide", description = "一方当前运行态摘要。")
	data class Side(
		@field:Schema(description = "Session 内稳定 sideId。")
		val sideId: String,
		@field:Schema(description = "当前上场成员 actorId。")
		val activeActorIds: List<String>,
		@field:Schema(description = "按固定阵容顺序排列的成员摘要。")
		val participants: List<Participant>,
	)

	/** 公开成员继续选择行动所需的 HP、状态、能力阶级和技能槽。 */
	@Schema(name = "BattleSessionParticipant", description = "Session 内成员运行态摘要。")
	data class Participant(
		@field:Schema(description = "Session 内稳定 actorId。")
		val actorId: String,
		@field:Schema(description = "精灵资料 Identifier。", type = "string")
		val creatureId: String,
		@field:Schema(description = "是否当前上场。")
		val active: Boolean,
		@field:Schema(description = "等级。")
		val level: Int,
		@field:Schema(description = "当前 HP。")
		val currentHp: Int,
		@field:Schema(description = "最大 HP。")
		val maxHp: Int,
		@field:Schema(description = "主要异常状态；没有时为空。", nullable = true)
		val majorStatus: String?,
		@field:Schema(description = "能力阶级变化。")
		val statStages: Map<String, Int>,
		@field:Schema(description = "技能槽运行态。")
		val skillSlots: List<SkillSlot>,
	)

	/** 公开服务端结算后的 PP，不携带完整技能规则。 */
	@Schema(name = "BattleSessionSkillSlot", description = "Session 内技能槽运行态。")
	data class SkillSlot(
		@field:Schema(description = "技能资料 Identifier。", type = "string")
		val skillId: String,
		@field:Schema(description = "技能名称。")
		val name: String,
		@field:Schema(description = "剩余 PP。")
		val remainingPp: Int,
		@field:Schema(description = "最大 PP。")
		val maxPp: Int,
	)

	/** 要求管理端从服务端提供的选项中为一个 actor 恰好选择一个行动。 */
	@Schema(name = "BattleSessionTurnRequirement", description = "一个 actor 的下一回合人工选择要求。")
	data class TurnRequirement(
		@field:Schema(description = "必须提交选择的 actorId。")
		val actorId: String,
		@field:Schema(description = "该 actor 当前允许提交的完整行动选项。")
		val options: List<BattleActionRequest>,
	)
}
