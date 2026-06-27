package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_BERRIES_TABLE = GameDataTableSpec(
	tableName = "game_berry",
	label = "树果资料",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 120),
		GameDataColumnSpec(name = "item_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "firmness_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "natural_gift_element_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "growth_time", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "max_harvest", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "natural_gift_power", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "size", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "smoothness", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "soil_dryness", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN, required = true),
	),
	searchColumns = listOf("code", "name"),
)

/**
 * 树果资料 Service。
 */
@Service
class GameBerriesService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_BERRIES_TABLE)
