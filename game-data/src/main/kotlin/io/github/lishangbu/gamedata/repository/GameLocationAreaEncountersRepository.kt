package io.github.lishangbu.gamedata.repository

import org.springframework.stereotype.Repository

private val GAME_LOCATION_AREA_ENCOUNTERS_TABLE = GameDataTableSpec(
	tableName = "game_location_area_encounter",
	label = "区域生物遭遇",
	columns = listOf(
		GameDataColumnSpec(name = "area_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "creature_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "method_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "min_level", type = GameDataColumnType.INT, required = true),
		GameDataColumnSpec(name = "max_level", type = GameDataColumnType.INT, required = true),
		GameDataColumnSpec(name = "chance", type = GameDataColumnType.INT, required = true),
		GameDataColumnSpec(name = "max_chance", type = GameDataColumnType.INT, required = true),
	),
	searchColumns = listOf("area_id", "creature_id"),
)

/**
 * 区域生物遭遇持久化访问。
 */
@Repository
class GameLocationAreaEncountersRepository(
	operations: GameDataJdbcOperations,
) : GameDataTableRepository(operations, GAME_LOCATION_AREA_ENCOUNTERS_TABLE)
