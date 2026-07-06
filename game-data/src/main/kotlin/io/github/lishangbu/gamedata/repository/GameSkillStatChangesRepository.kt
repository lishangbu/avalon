package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

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
	sqlClient: KSqlClient,
) : GameDataJimmerRepository(sqlClient, GAME_SKILL_STAT_CHANGES_TABLE) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPage<GameDataRecordResponse> =
		listRecords(page, size, query, filters)

	@Transactional(readOnly = true)
	fun get(id: Long): GameDataRecordResponse =
		getRecord(id)

	@Transactional
	fun create(request: GameDataRecordRequest): GameDataRecordResponse =
		createRecord(request)

	@Transactional
	fun update(id: Long, request: GameDataRecordRequest): GameDataRecordResponse =
		updateRecord(id, request)

	@Transactional
	fun delete(id: Long) {
		deleteRecord(id)
	}
}
