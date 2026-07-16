package io.github.lishangbu.match.trainer

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table
import org.babyfish.jimmer.sql.Version
import java.time.Instant

/** 每个 Trainer 唯一的可编辑 Team 聚合根。 */
@Entity
@Table(name = "match_trainer_team")
interface MatchTrainerTeam {
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	/** Team 所属 Trainer；数据库唯一约束保证一个 Trainer 只有一支 Team。 */
	val trainerId: Long
	val name: String
	val nameKey: String
	val active: Boolean
	/** 完整替换命令使用的乐观并发版本。 */
	@Version
	val revision: Long
	val createdAt: Instant
	val updatedAt: Instant
}
