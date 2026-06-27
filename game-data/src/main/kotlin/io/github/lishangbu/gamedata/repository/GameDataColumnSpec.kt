package io.github.lishangbu.gamedata.repository

/**
 * 单个资料表字段的写入约束。
 */
data class GameDataColumnSpec(
	val name: String,
	val type: GameDataColumnType,
	val required: Boolean = false,
	val writable: Boolean = true,
	val maxLength: Int? = null,
)
