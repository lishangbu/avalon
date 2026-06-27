package io.github.lishangbu.gamedata.repository

import org.springframework.stereotype.Repository

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
 * 个体特征取值持久化访问。
 */
@Repository
class GameCharacteristicValuesRepository(
	operations: GameDataJdbcOperations,
) : GameDataTableRepository(operations, GAME_CHARACTERISTIC_VALUES_TABLE)
