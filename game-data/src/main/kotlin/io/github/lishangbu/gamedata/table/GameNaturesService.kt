package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_NATURES_TABLE = GameDataTableSpec(
	tableName = "game_nature",
	label = "性格资料",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 120),
		GameDataColumnSpec(name = "increased_stat_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "decreased_stat_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "likes_flavor_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "hates_flavor_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN, required = true),
	),
	searchColumns = listOf("code", "name"),
)

/**
 * 性格资料 Service。
 */
@Service
class GameNaturesService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_NATURES_TABLE)
