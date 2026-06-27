package io.github.lishangbu.gamedata.repository

import org.springframework.stereotype.Repository

private val GAME_SKILL_AILMENTS_TABLE = GameDataTableSpec(
	tableName = "game_skill_ailment",
	label = "技能异常",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 120),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN, required = true),
	),
	searchColumns = listOf("code", "name"),
)

/**
 * 技能异常持久化访问。
 */
@Repository
class GameSkillAilmentsRepository(
	operations: GameDataJdbcOperations,
) : GameDataTableRepository(operations, GAME_SKILL_AILMENTS_TABLE)
