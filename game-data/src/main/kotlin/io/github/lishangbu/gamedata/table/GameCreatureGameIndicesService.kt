package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_CREATURE_GAME_INDICES_TABLE = GameDataTableSpec(
	tableName = "game_creature_game_index",
	label = "生物版本索引",
	columns = listOf(
		GameDataColumnSpec(name = "creature_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "version_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "game_index", type = GameDataColumnType.INT, required = true),
	),
	searchColumns = listOf("creature_id", "version_id"),
)

/**
 * 生物版本索引 Service。
 */
@Service
class GameCreatureGameIndicesService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_CREATURE_GAME_INDICES_TABLE)
