package io.github.lishangbu.gamedata.model

data class GameDataPage<T>(
	val rows: List<T>,
	val totalRowCount: Long,
	val totalPageCount: Int,
	val page: Int,
	val size: Int,
)
