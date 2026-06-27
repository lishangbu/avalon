package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_SKILL_DAMAGE_CLASS_TABLE = GameDataTableSpec(
	tableName = "game_skill_damage_class",
	label = "技能分类",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "description", type = GameDataColumnType.STRING, maxLength = 500),
		GameDataColumnSpec(name = "sort_order", type = GameDataColumnType.INT, required = true),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN),
	),
	searchColumns = listOf("code", "name", "description"),
)

/**
 * 技能分类 Service。
 */
@Service
class GameSkillDamageClassService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_SKILL_DAMAGE_CLASS_TABLE)
