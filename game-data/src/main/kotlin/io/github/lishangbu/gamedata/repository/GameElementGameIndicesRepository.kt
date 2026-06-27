package io.github.lishangbu.gamedata.repository

import org.springframework.stereotype.Repository

private val GAME_ELEMENT_GAME_INDICES_TABLE = GameDataTableSpec(
	tableName = "game_element_game_index",
	label = "属性索引",
	columns = listOf(
		GameDataColumnSpec(name = "element_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "game_index", type = GameDataColumnType.INT, required = true),
	),
	searchColumns = listOf("element_id"),
)

/**
 * 属性索引持久化访问。
 */
@Repository
class GameElementGameIndicesRepository(
	operations: GameDataJdbcOperations,
) : GameDataTableRepository(operations, GAME_ELEMENT_GAME_INDICES_TABLE)
