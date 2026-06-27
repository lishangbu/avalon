package io.github.lishangbu.gamedata.repository

import org.springframework.stereotype.Repository

private val GAME_SKILL_TARGETS_TABLE = GameDataTableSpec(
	tableName = "game_skill_target",
	label = "技能目标",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 120),
		GameDataColumnSpec(name = "description", type = GameDataColumnType.STRING),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN, required = true),
	),
	searchColumns = listOf("code", "name", "description"),
)

/**
 * 技能目标持久化访问。
 */
@Repository
class GameSkillTargetsRepository(
	operations: GameDataJdbcOperations,
) : GameDataTableRepository(operations, GAME_SKILL_TARGETS_TABLE)
