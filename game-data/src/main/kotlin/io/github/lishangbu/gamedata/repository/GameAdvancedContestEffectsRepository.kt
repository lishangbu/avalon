package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

private val GAME_ADVANCED_CONTEST_EFFECTS_TABLE = GameDataTableSpec(
	tableName = "game_advanced_contest_effect",
	label = "高级评价效果",
	columns = listOf(
		GameDataColumnSpec(name = "appeal", type = GameDataColumnType.INT, required = true),
		GameDataColumnSpec(name = "flavor_text", type = GameDataColumnType.STRING),
	),
	searchColumns = listOf("flavor_text"),
)

/**
 * 高级评价效果持久化访问。
 */
@Repository
class GameAdvancedContestEffectsRepository(
	sqlClient: KSqlClient,
) : GameDataJimmerRepository(sqlClient, GAME_ADVANCED_CONTEST_EFFECTS_TABLE) {
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
