package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_CREATURE_FORMS_TABLE = GameDataTableSpec(
	tableName = "game_creature_form",
	label = "生物形态",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 120),
		GameDataColumnSpec(name = "creature_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "version_group_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "form_name", type = GameDataColumnType.STRING, maxLength = 120),
		GameDataColumnSpec(name = "sort_order", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "form_order", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "battle_only", type = GameDataColumnType.BOOLEAN, required = true),
		GameDataColumnSpec(name = "default_form", type = GameDataColumnType.BOOLEAN, required = true),
		GameDataColumnSpec(name = "enhanced_form", type = GameDataColumnType.BOOLEAN, required = true),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN, required = true),
	),
	searchColumns = listOf("code", "name", "form_name"),
)

/**
 * 生物形态 Service。
 */
@Service
class GameCreatureFormsService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_CREATURE_FORMS_TABLE)
