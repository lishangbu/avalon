package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.springframework.stereotype.Repository

private val GAME_CREATURE_SKILL_LEARNS_TABLE = GameDataTableSpec(
	tableName = "game_creature_skill_learn",
	label = "精灵技能学习",
	columns = listOf(
		GameDataColumnSpec(name = "creature_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "skill_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "learn_method_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "level_learned_at", type = GameDataColumnType.INT, required = true),
	),
	searchColumns = listOf("creature_id", "skill_id"),
)

/**
 * 精灵技能学习持久化访问。
 */
@Repository
class GameCreatureSkillLearnsRepository(
	private val operations: GameDataJimmerOperations,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPage<GameDataRecordResponse> =
		operations.list(GAME_CREATURE_SKILL_LEARNS_TABLE, page, size, query, filters)

	fun get(id: Long): GameDataRecordResponse =
		operations.get(GAME_CREATURE_SKILL_LEARNS_TABLE, id)

	fun create(request: GameDataRecordRequest): GameDataRecordResponse =
		operations.create(GAME_CREATURE_SKILL_LEARNS_TABLE, request)

	fun update(id: Long, request: GameDataRecordRequest): GameDataRecordResponse =
		operations.update(GAME_CREATURE_SKILL_LEARNS_TABLE, id, request)

	fun delete(id: Long) {
		operations.delete(GAME_CREATURE_SKILL_LEARNS_TABLE, id)
	}
}
