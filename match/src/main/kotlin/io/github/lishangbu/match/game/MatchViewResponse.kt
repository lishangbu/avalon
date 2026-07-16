package io.github.lishangbu.match.game

import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import java.time.Instant

/** 按当前 Trainer 投影的 Match View，不包含内部 side 与 Battle Session 标识。 */
@Immutable
interface MatchViewResponse {
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	val ruleCode: String
	val status: MatchStatus
	val revision: Long
	val turnNumber: Int
	val previewDeadline: Instant?
	val turnDeadline: Instant?
	val battleDeadline: Instant?
	/** 仅返回查看者自己已锁定的首发位置；对手选择始终保密到 Runtime 启动。 */
	val leadPosition: Int?
	val result: String?
	val completionReason: MatchCompletionReason?
	val interruptionReason: MatchInterruptionReason?
	val sides: List<MatchViewSideResponse>
	val requirements: List<MatchTurnRequirementResponse>
	val events: List<MatchBattleEvent>
}
