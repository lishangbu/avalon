package io.github.lishangbu.gamedata.repository

import org.springframework.stereotype.Repository

private val GAME_SKILL_CONTEST_COMBOS_TABLE = GameDataTableSpec(
	tableName = "game_skill_contest_combo",
	label = "技能评价组合",
	columns = listOf(
		GameDataColumnSpec(name = "skill_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "combo_type", type = GameDataColumnType.STRING, required = true, maxLength = 40),
		GameDataColumnSpec(name = "relation_type", type = GameDataColumnType.STRING, required = true, maxLength = 40),
		GameDataColumnSpec(name = "related_skill_id", type = GameDataColumnType.LONG, required = true),
	),
	searchColumns = listOf("skill_id", "related_skill_id"),
)

/**
 * 技能评价组合持久化访问。
 */
@Repository
class GameSkillContestCombosRepository(
	operations: GameDataJdbcOperations,
) : GameDataTableRepository(operations, GAME_SKILL_CONTEST_COMBOS_TABLE)
