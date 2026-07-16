package io.github.lishangbu.gamedata.catalog

import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/** 玩家组队和资料浏览使用的启用 Creature 聚合。 */
@Immutable
interface PlayerCatalogCreatureResponse {
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	val code: String
	val name: String
	val genus: String?
	val flavorText: String?
	val height: Int?
	val weight: Int?
	val defaultSkin: PlayerCatalogSkinResponse
}
