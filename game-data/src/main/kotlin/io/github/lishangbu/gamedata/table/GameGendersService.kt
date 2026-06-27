package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_GENDERS_TABLE = GameDataTableSpec(
	tableName = "game_gender",
	label = "性别资料",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 120),
		GameDataColumnSpec(name = "sort_order", type = GameDataColumnType.INT, required = true),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN, required = true),
	),
	searchColumns = listOf("code", "name"),
)

/**
 * 性别资料 Service。
 */
@Service
class GameGendersService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_GENDERS_TABLE)
