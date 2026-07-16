package io.github.lishangbu.gamedata.catalog

import io.github.lishangbu.gamedata.entity.ItemUsageType
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/** 玩家目录公开的道具用途、图标与文本。 */
@Immutable
interface PlayerCatalogItemResponse {
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	val code: String
	val name: String
	val usageType: ItemUsageType
	val iconAssetKey: String
	val cost: Int?
	val shortEffect: String?
	val effect: String?
	val flavorText: String?
}
