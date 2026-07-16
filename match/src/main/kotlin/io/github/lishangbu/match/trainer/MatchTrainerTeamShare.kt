package io.github.lishangbu.match.trainer

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Serialized
import org.babyfish.jimmer.sql.Table
import java.time.Instant

/** 持久化的不可变 Team 分享快照。 */
@Entity
@Table(name = "match_trainer_team_share")
interface MatchTrainerTeamShare {
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	val teamId: Long
	val code: String
	val teamRevision: Long
	@Serialized
	val snapshot: TrainerTeamShareSnapshot
	val createdAt: Instant
}
