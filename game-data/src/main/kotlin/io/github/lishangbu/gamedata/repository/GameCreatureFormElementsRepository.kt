package io.github.lishangbu.gamedata.repository

import org.springframework.stereotype.Repository

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
 * 生物形态属性持久化访问。
 */
@Repository
class GameCreatureFormElementsRepository(
	operations: GameDataJdbcOperations,
) : GameDataTableRepository(operations, GAME_CREATURE_FORM_ELEMENTS_TABLE)
