package io.github.lishangbu.gamedata.repository

import org.springframework.stereotype.Repository

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
 * 技能资料持久化访问。
 */
@Repository
class GameSkillRepository(
	operations: GameDataJdbcOperations,
) : GameDataTableRepository(operations, GAME_SKILL_TABLE)
