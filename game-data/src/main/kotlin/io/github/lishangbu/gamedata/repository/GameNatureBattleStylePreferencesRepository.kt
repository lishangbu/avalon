package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

private val GAME_NATURE_BATTLE_STYLE_PREFERENCES_TABLE = GameDataTableSpec(
	tableName = "game_nature_battle_style_preference",
	label = "性格战斗风格偏好",
	columns = listOf(
		GameDataColumnSpec(name = "nature_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "battle_style_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "low_hp_preference", type = GameDataColumnType.INT, required = true),
		GameDataColumnSpec(name = "high_hp_preference", type = GameDataColumnType.INT, required = true),
	),
	searchColumns = listOf("nature_id", "battle_style_id"),
)

/**
 * 性格战斗风格偏好持久化访问。
 */
@Repository
class GameNatureBattleStylePreferencesRepository(
	sqlClient: KSqlClient,
) : GameDataJimmerRepository(sqlClient, GAME_NATURE_BATTLE_STYLE_PREFERENCES_TABLE) {
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
