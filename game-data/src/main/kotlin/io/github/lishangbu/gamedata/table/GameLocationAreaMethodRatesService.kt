package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_LOCATION_AREA_METHOD_RATES_TABLE = GameDataTableSpec(
	tableName = "game_location_area_method_rate",
	label = "区域遭遇方式概率",
	columns = listOf(
		GameDataColumnSpec(name = "area_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "method_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "version_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "rate", type = GameDataColumnType.INT, required = true),
	),
	searchColumns = listOf("area_id", "method_id", "version_id"),
)

/**
 * 区域遭遇方式概率 Service。
 */
@Service
class GameLocationAreaMethodRatesService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_LOCATION_AREA_METHOD_RATES_TABLE)
