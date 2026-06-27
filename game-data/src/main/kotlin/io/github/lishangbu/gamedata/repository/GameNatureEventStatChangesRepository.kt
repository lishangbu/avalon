package io.github.lishangbu.gamedata.repository

import org.springframework.stereotype.Repository

private val GAME_NATURE_EVENT_STAT_CHANGES_TABLE = GameDataTableSpec(
	tableName = "game_nature_event_stat_change",
	label = "性格活动能力变化",
	columns = listOf(
		GameDataColumnSpec(name = "nature_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "event_stat_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "max_change", type = GameDataColumnType.INT, required = true),
	),
	searchColumns = listOf("nature_id", "event_stat_id"),
)

/**
 * 性格活动能力变化持久化访问。
 */
@Repository
class GameNatureEventStatChangesRepository(
	operations: GameDataJdbcOperations,
) : GameDataTableRepository(operations, GAME_NATURE_EVENT_STAT_CHANGES_TABLE)
