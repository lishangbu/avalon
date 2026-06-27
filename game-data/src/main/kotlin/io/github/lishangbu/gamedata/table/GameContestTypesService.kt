package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_CONTEST_TYPES_TABLE = GameDataTableSpec(
	tableName = "game_contest_type",
	label = "评分类别",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 120),
		GameDataColumnSpec(name = "color", type = GameDataColumnType.STRING, maxLength = 40),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN, required = true),
	),
	searchColumns = listOf("code", "name"),
)

/**
 * 评分类别 Service。
 */
@Service
class GameContestTypesService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_CONTEST_TYPES_TABLE)
