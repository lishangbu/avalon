package io.github.lishangbu.gamedata.repository

import org.springframework.stereotype.Repository

private val GAME_EVENT_STAT_NATURE_EFFECTS_TABLE = GameDataTableSpec(
	tableName = "game_event_stat_nature_effect",
	label = "活动能力性格影响",
	columns = listOf(
		GameDataColumnSpec(name = "event_stat_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "nature_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "effect_type", type = GameDataColumnType.STRING, required = true, maxLength = 20),
	),
	searchColumns = listOf("event_stat_id", "nature_id"),
)

/**
 * 活动能力性格影响持久化访问。
 */
@Repository
class GameEventStatNatureEffectsRepository(
	operations: GameDataJdbcOperations,
) : GameDataTableRepository(operations, GAME_EVENT_STAT_NATURE_EFFECTS_TABLE)
