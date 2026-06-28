package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.springframework.stereotype.Repository

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
	private val operations: GameDataJdbcOperations,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPage<GameDataRecordResponse> =
		operations.list(GAME_NATURE_BATTLE_STYLE_PREFERENCES_TABLE, page, size, query, filters)

	fun get(id: Long): GameDataRecordResponse =
		operations.get(GAME_NATURE_BATTLE_STYLE_PREFERENCES_TABLE, id)

	fun create(request: GameDataRecordRequest): GameDataRecordResponse =
		operations.create(GAME_NATURE_BATTLE_STYLE_PREFERENCES_TABLE, request)

	fun update(id: Long, request: GameDataRecordRequest): GameDataRecordResponse =
		operations.update(GAME_NATURE_BATTLE_STYLE_PREFERENCES_TABLE, id, request)

	fun delete(id: Long) {
		operations.delete(GAME_NATURE_BATTLE_STYLE_PREFERENCES_TABLE, id)
	}
}
