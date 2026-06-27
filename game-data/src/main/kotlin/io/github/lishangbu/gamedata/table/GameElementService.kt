package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_ELEMENT_TABLE = GameDataTableSpec(
	tableName = "game_element",
	label = "属性资料",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "sort_order", type = GameDataColumnType.INT, required = true),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN),
	),
	searchColumns = listOf("code", "name"),
)

/**
 * 属性资料 Service。
 */
@Service
class GameElementService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_ELEMENT_TABLE)
