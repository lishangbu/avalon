package io.github.lishangbu.match.trainer

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table
import java.time.Instant

/**
 * Trainer 归档事务读取和取消 Pending Challenge 所需的持久化投影。
 *
 * 完整 Challenge 行为由后续 Challenge 应用服务拥有；此处仅暴露归档一致性边界需要的字段。
 */
@Entity
@Table(name = "match_challenge")
interface MatchChallenge {
	@Id
	val id: Long
	/** 发起 Challenge 的 Trainer。 */
	val challengerTrainerId: Long
	/** 接收 Challenge 的 Trainer。 */
	val challengedTrainerId: Long
	/** 当前 Challenge 生命周期状态。 */
	val status: io.github.lishangbu.match.challenge.ChallengeStatus
	/** 仅取消终态携带的业务原因。 */
	val cancellationReason: io.github.lishangbu.match.challenge.ChallengeCancellationReason?
	/** Challenge 进入终态的时间。 */
	val resolvedAt: Instant?
	/** 条件更新和并发裁决使用的版本。 */
	val revision: Long
	/** 最后一次状态迁移时间。 */
	val updatedAt: Instant
}
