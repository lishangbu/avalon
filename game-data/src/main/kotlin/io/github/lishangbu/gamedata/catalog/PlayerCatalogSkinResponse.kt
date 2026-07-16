package io.github.lishangbu.gamedata.catalog

import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/** 玩家目录公开的纯外观与稳定资源键。 */
@Immutable
interface PlayerCatalogSkinResponse {
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	val code: String
	val name: String
	val avatarAssetKey: String
	val frontAssetKey: String
	val backAssetKey: String
}
