package io.github.lishangbu.match.game

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.sql.*
import java.time.Instant

/** 真人 Match 聚合根；Battle Session 仅作为临时 Runtime 引用，不对玩家公开。 */
@Entity
@Table(name = "match_game")
interface MatchGame {
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	val id: Long
	val challengeId: Long
	val ruleCode: String
	val status: MatchStatus
	val battleSessionId: String?
	@Version
	val revision: Long
	val turnNumber: Int
	val turnDeadline: Instant?
	/** Runtime 消失后仍可重建终态 Match View 的精简 JSONB 投影。 */
	@Serialized
	val viewState: MatchBattleViewState?
	val interruptionReason: MatchInterruptionReason?
	val outcome: MatchOutcome?
	val completionReason: MatchCompletionReason?
	val winnerTrainerId: Long?
	val battleReason: String?
	val startedAt: Instant?
	val endedAt: Instant?
	val createdAt: Instant
	val updatedAt: Instant
}
