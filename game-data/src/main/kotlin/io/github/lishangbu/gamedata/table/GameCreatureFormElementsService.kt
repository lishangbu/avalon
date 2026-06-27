package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_CREATURE_FORM_ELEMENTS_TABLE = GameDataTableSpec(
	tableName = "game_creature_form_element",
	label = "生物形态属性",
	columns = listOf(
		GameDataColumnSpec(name = "form_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "element_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "slot_order", type = GameDataColumnType.INT, required = true),
	),
	searchColumns = listOf("form_id", "element_id"),
)

/**
 * 生物形态属性 Service。
 */
@Service
class GameCreatureFormElementsService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_CREATURE_FORM_ELEMENTS_TABLE)
