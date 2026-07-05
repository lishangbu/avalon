package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
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
	private val operations: GameDataJimmerOperations,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPage<GameDataRecordResponse> =
		operations.list(GAME_SKILL_STAT_CHANGES_TABLE, page, size, query, filters)

	fun get(id: Long): GameDataRecordResponse =
		operations.get(GAME_SKILL_STAT_CHANGES_TABLE, id)

	fun create(request: GameDataRecordRequest): GameDataRecordResponse =
		operations.create(GAME_SKILL_STAT_CHANGES_TABLE, request)

	fun update(id: Long, request: GameDataRecordRequest): GameDataRecordResponse =
		operations.update(GAME_SKILL_STAT_CHANGES_TABLE, id, request)

	fun delete(id: Long) {
		operations.delete(GAME_SKILL_STAT_CHANGES_TABLE, id)
	}
}
