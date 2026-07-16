package io.github.lishangbu.gamedata.catalog

import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/** 玩家目录公开的技能参数、精确效果与世界观文本。 */
@Immutable
interface PlayerCatalogSkillResponse {
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	val code: String
	val name: String
	@JsonConverter(LongToStringConverter::class)
	val elementId: Long?
	val accuracy: Int?
	val power: Int?
	val pp: Int?
	val priority: Int?
	val shortEffect: String?
	val effect: String?
	val flavorText: String?
}
