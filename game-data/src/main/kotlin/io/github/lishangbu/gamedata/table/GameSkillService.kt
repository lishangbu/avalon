package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_SKILL_TABLE = GameDataTableSpec(
	tableName = "game_skill",
	label = "技能资料",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 120),
		GameDataColumnSpec(name = "element_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "damage_class_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "accuracy", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "power", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "pp", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "priority", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "effect_chance", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN),
	),
	searchColumns = listOf("code", "name"),
)

/**
 * 技能资料 Service。
 */
@Service
class GameSkillService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_SKILL_TABLE)
