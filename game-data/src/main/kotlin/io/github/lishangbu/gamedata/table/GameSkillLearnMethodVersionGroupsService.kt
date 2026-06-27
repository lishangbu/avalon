package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_SKILL_LEARN_METHOD_VERSION_GROUPS_TABLE = GameDataTableSpec(
	tableName = "game_skill_learn_method_version_group",
	label = "学习方式版本组",
	columns = listOf(
		GameDataColumnSpec(name = "learn_method_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "version_group_id", type = GameDataColumnType.LONG, required = true),
	),
	searchColumns = listOf("learn_method_id", "version_group_id"),
)

/**
 * 学习方式版本组 Service。
 */
@Service
class GameSkillLearnMethodVersionGroupsService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_SKILL_LEARN_METHOD_VERSION_GROUPS_TABLE)
