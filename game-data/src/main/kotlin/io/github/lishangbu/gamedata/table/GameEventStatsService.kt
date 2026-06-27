package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_EVENT_STATS_TABLE = GameDataTableSpec(
	tableName = "game_event_stat",
	label = "活动能力项",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 120),
		GameDataColumnSpec(name = "sort_order", type = GameDataColumnType.INT, required = true),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN, required = true),
	),
	searchColumns = listOf("code", "name"),
)

/**
 * 活动能力项 Service。
 */
@Service
class GameEventStatsService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_EVENT_STATS_TABLE)
