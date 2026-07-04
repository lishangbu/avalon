package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.springframework.stereotype.Repository

private val GAME_CREATURE_ABILITY_TABLE = GameDataTableSpec(
	tableName = "game_creature_ability",
	label = "精灵特性绑定",
	columns = listOf(
		GameDataColumnSpec(name = "creature_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "ability_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "slot_order", type = GameDataColumnType.INT, required = true),
		GameDataColumnSpec(name = "hidden", type = GameDataColumnType.BOOLEAN),
	),
	searchColumns = listOf("creature_id", "ability_id"),
)

/**
 * 精灵特性绑定持久化访问。
 */
@Repository
class GameCreatureAbilityRepository(
	private val operations: GameDataJdbcOperations,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPage<GameDataRecordResponse> =
		operations.list(GAME_CREATURE_ABILITY_TABLE, page, size, query, filters)

	fun get(id: Long): GameDataRecordResponse =
		operations.get(GAME_CREATURE_ABILITY_TABLE, id)

	fun create(request: GameDataRecordRequest): GameDataRecordResponse =
		operations.create(GAME_CREATURE_ABILITY_TABLE, request)

	fun update(id: Long, request: GameDataRecordRequest): GameDataRecordResponse =
		operations.update(GAME_CREATURE_ABILITY_TABLE, id, request)

	fun delete(id: Long) {
		operations.delete(GAME_CREATURE_ABILITY_TABLE, id)
	}
}
