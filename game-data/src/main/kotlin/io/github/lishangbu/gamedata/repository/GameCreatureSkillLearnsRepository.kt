package io.github.lishangbu.gamedata.repository

import org.springframework.stereotype.Repository

private val GAME_CREATURE_SKILL_LEARNS_TABLE = GameDataTableSpec(
	tableName = "game_creature_skill_learn",
	label = "生物技能学习",
	columns = listOf(
		GameDataColumnSpec(name = "creature_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "skill_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "learn_method_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "level_learned_at", type = GameDataColumnType.INT, required = true),
	),
	searchColumns = listOf("creature_id", "skill_id"),
)

/**
 * 生物技能学习持久化访问。
 */
@Repository
class GameCreatureSkillLearnsRepository(
	operations: GameDataJdbcOperations,
) : GameDataTableRepository(operations, GAME_CREATURE_SKILL_LEARNS_TABLE)
