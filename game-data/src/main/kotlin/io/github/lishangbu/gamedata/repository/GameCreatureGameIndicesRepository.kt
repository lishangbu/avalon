package io.github.lishangbu.gamedata.repository

import org.springframework.stereotype.Repository

private val GAME_CREATURE_GAME_INDICES_TABLE = GameDataTableSpec(
	tableName = "game_creature_game_index",
	label = "生物索引",
	columns = listOf(
		GameDataColumnSpec(name = "creature_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "game_index", type = GameDataColumnType.INT, required = true),
	),
	searchColumns = listOf("creature_id"),
)

/**
 * 生物索引持久化访问。
 */
@Repository
class GameCreatureGameIndicesRepository(
	operations: GameDataJdbcOperations,
) : GameDataTableRepository(operations, GAME_CREATURE_GAME_INDICES_TABLE)
