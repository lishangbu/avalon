package io.github.lishangbu.match.trainer

import io.github.lishangbu.battleengine.model.BattleGender
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongListToStringListConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/** 玩家 Team API 返回的单个成员。 */
@Immutable
interface TrainerTeamMemberResponse {
	@JsonConverter(LongToStringConverter::class)
	val creatureId: Long
	val gender: BattleGender
	@JsonConverter(LongToStringConverter::class)
	val skinId: Long
	@JsonConverter(LongListToStringListConverter::class)
	val skillIds: List<Long>
	@JsonConverter(LongToStringConverter::class)
	val abilityId: Long
	@JsonConverter(LongToStringConverter::class)
	val itemId: Long
	@JsonConverter(LongToStringConverter::class)
	val natureId: Long
	@JsonConverter(LongToStringConverter::class)
	val teraElementId: Long
	val individualValues: Map<String, Int>
	val effortValues: Map<String, Int>
}

/** 将内部成员快照映射为 Identifier 字符串化的 API 视图。 */
internal fun TrainerTeamMemberRecord.toResponse() = TrainerTeamMemberResponse {
	creatureId = this@toResponse.creatureId
	gender = this@toResponse.gender
	skinId = this@toResponse.skinId
	skillIds = this@toResponse.skillIds
	abilityId = this@toResponse.abilityId
	itemId = this@toResponse.itemId
	natureId = this@toResponse.natureId
	teraElementId = this@toResponse.teraElementId
	individualValues = this@toResponse.individualValues
	effortValues = this@toResponse.effortValues
}
