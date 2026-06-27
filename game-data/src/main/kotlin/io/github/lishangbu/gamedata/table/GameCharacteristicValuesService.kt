package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_CHARACTERISTIC_VALUES_TABLE = GameDataTableSpec(
	tableName = "game_characteristic_value",
	label = "个体特征取值",
	columns = listOf(
		GameDataColumnSpec(name = "characteristic_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "possible_value", type = GameDataColumnType.INT, required = true),
	),
	searchColumns = listOf("characteristic_id", "possible_value"),
)

/**
 * 个体特征取值 Service。
 */
@Service
class GameCharacteristicValuesService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_CHARACTERISTIC_VALUES_TABLE)
