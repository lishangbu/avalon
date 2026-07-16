package io.github.lishangbu.gamedata.catalog

import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/** 玩家目录公开的特性效果与世界观文本。 */
@Immutable
interface PlayerCatalogAbilityResponse {
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	val code: String
	val name: String
	val shortEffect: String?
	val effect: String?
	val flavorText: String?
}
