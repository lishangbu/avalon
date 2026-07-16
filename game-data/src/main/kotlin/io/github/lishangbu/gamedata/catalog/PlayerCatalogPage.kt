package io.github.lishangbu.gamedata.catalog

/** 玩家目录统一分页响应；total 表示符合条件的总记录数。 */
data class PlayerCatalogPage<T>(
	val items: List<T>,
	val total: Long,
)
