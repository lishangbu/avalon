package io.github.lishangbu.gamedata.repository

import org.springframework.stereotype.Repository

private val GAME_SKILL_STAT_CHANGES_TABLE = GameDataTableSpec(
	tableName = "game_skill_stat_change",
	label = "技能数值变化",
	columns = listOf(
		GameDataColumnSpec(name = "skill_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "stat_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "change_value", type = GameDataColumnType.INT, required = true),
	),
	searchColumns = listOf("skill_id", "stat_id"),
)

/**
 * 技能数值变化持久化访问。
 */
@Repository
class GameSkillStatChangesRepository(
	operations: GameDataJdbcOperations,
) : GameDataTableRepository(operations, GAME_SKILL_STAT_CHANGES_TABLE)
