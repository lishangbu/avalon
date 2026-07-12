package io.github.lishangbu.match.trainer

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table
import org.babyfish.jimmer.sql.Version
import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
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
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	val id: Long
	/** 发起命令 UUID，用于账户内幂等。 */
	val commandId: String
	/** 发起 Challenge 的 Trainer。 */
	val challengerTrainerId: Long
	/** 接收 Challenge 的 Trainer。 */
	val challengedTrainerId: Long
	/** Challenge 创建时冻结的双方公开名称，历史视图不回查外部状态。 */
	val challengerDisplayName: String
	val challengedDisplayName: String
	val challengerAccountId: Long
	val challengedAccountId: Long
	val challengerSnapshotId: Long
	val challengedSnapshotId: Long?
	/** 首版固定为 standard-single，但仍持久化以冻结规则语义。 */
	val ruleCode: String
	/** 当前 Challenge 生命周期状态。 */
	val status: io.github.lishangbu.match.challenge.ChallengeStatus
	/** 仅取消终态携带的业务原因。 */
	val cancellationReason: io.github.lishangbu.match.challenge.ChallengeCancellationReason?
	/** Challenge 进入终态的时间。 */
	val resolvedAt: Instant?
	/** 条件更新和并发裁决使用的版本。 */
	@Version
	val revision: Long
	/** 创建后固定的五分钟到期时间，查询和命令都不会续期。 */
	val expiresAt: Instant
	val createdAt: Instant
	/** 最后一次状态迁移时间。 */
	val updatedAt: Instant
}
