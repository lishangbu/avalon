package io.github.lishangbu.match.trainer

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Serialized
import org.babyfish.jimmer.sql.Table

/** Team 中按位置排序的一名完整成员；等级在真人 Match 构造时固定为 50，不在此处持久化。 */
@Entity
@Table(name = "match_trainer_team_member")
interface MatchTrainerTeamMember {
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	val teamId: Long
	/** 从 1 开始的稳定阵容位置。 */
	val position: Int
	val creatureId: Long
	val skinId: Long
	val abilityId: Long
	val itemId: Long
	val natureId: Long
	val teraElementId: Long
	/** 六项完整 IV；请求缺失项在写入前补为 31。 */
	@Serialized
	val individualValues: Map<String, Int>
	/** 六项完整 EV；请求缺失项在写入前补为 0。 */
	@Serialized
	val effortValues: Map<String, Int>
}
