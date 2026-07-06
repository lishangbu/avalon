package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

private val GAME_NATURE_EVENT_STAT_CHANGES_TABLE = GameDataTableSpec(
	tableName = "game_nature_event_stat_change",
	label = "性格活动能力变化",
	columns = listOf(
		GameDataColumnSpec(name = "nature_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "event_stat_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "max_change", type = GameDataColumnType.INT, required = true),
	),
	searchColumns = listOf("nature_id", "event_stat_id"),
)

/**
 * 性格活动能力变化持久化访问。
 */
@Repository
class GameNatureEventStatChangesRepository(
	sqlClient: KSqlClient,
) : GameDataJimmerRepository(sqlClient, GAME_NATURE_EVENT_STAT_CHANGES_TABLE) {
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
