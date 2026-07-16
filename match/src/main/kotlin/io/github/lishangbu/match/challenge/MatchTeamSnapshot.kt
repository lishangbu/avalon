package io.github.lishangbu.match.challenge

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Serialized
import org.babyfish.jimmer.sql.Table
import java.time.Instant

/** Challenge/Match 共用的不可变 Trainer Team JSONB 快照。 */
@Entity
@Table(name = "match_team_snapshot")
interface MatchTeamSnapshot {
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	val id: Long
	val trainerId: Long
	val sourceTeamId: Long
	/** 快照结构版本；读取旧 Match 时据此选择兼容解析方式。 */
	val schemaVersion: Int
	@Serialized
	val roster: TrainerTeamSnapshotRoster
	val createdAt: Instant
}
