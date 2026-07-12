package io.github.lishangbu.match.game

import com.fasterxml.jackson.annotation.JsonInclude
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongListToStringListConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/** 一侧内以 position 定位的成员；隐藏字段在对方视角保持 null。 */
@Immutable
@JsonInclude(JsonInclude.Include.NON_NULL)
interface MatchViewParticipantResponse {
	val position: Int
	@JsonConverter(LongToStringConverter::class)
	val creatureId: Long
	val active: Boolean
	val currentHp: Int
	val maxHp: Int
	val level: Int?
	@JsonConverter(LongListToStringListConverter::class)
	val skillIds: List<Long>?
	@JsonConverter(LongToStringConverter::class)
	val abilityId: Long?
	@JsonConverter(LongToStringConverter::class)
	val itemId: Long?
	@JsonConverter(LongToStringConverter::class)
	val natureId: Long?
	val individualValues: Map<String, Int>?
	val effortValues: Map<String, Int>?
}
