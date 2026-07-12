package io.github.lishangbu.match.trainer

import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/** 当前 Trainer 的完整可编辑 Team 资源。 */
@Immutable
interface TrainerTeamResponse {
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@JsonConverter(LongToStringConverter::class)
	val trainerId: Long
	val revision: Long
	val members: List<TrainerTeamMemberResponse>
}

/** 将内部 Team 聚合收敛为玩家 API 资源。 */
internal fun TrainerTeamRecord.toResponse() = TrainerTeamResponse {
	id = this@toResponse.id
	trainerId = this@toResponse.trainerId
	revision = this@toResponse.revision
	members = this@toResponse.members.map(TrainerTeamMemberRecord::toResponse)
}
