package io.github.lishangbu.gamedata.repository

import org.springframework.stereotype.Repository

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
 * 评分类别持久化访问。
 */
@Repository
class GameContestTypesRepository(
	operations: GameDataJdbcOperations,
) : GameDataTableRepository(operations, GAME_CONTEST_TYPES_TABLE)
