package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.springframework.stereotype.Repository

private val GAME_EVOLUTION_DETAILS_TABLE = GameDataTableSpec(
	tableName = "game_evolution_detail",
	label = "进化条件",
	columns = listOf(
		GameDataColumnSpec(name = "chain_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "from_species_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "to_species_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "trigger_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "item_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "held_item_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "known_skill_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "known_element_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "location_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "party_species_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "party_element_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "trade_species_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "gender_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "region_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "min_level", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "min_happiness", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "min_beauty", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "min_affection", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "relative_physical_stats", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "min_damage_taken", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "min_move_count", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "min_steps", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "time_of_day", type = GameDataColumnType.STRING, maxLength = 40),
		GameDataColumnSpec(name = "needs_overworld_rain", type = GameDataColumnType.BOOLEAN),
		GameDataColumnSpec(name = "turn_upside_down", type = GameDataColumnType.BOOLEAN),
		GameDataColumnSpec(name = "near_special_rock", type = GameDataColumnType.BOOLEAN),
		GameDataColumnSpec(name = "needs_multiplayer", type = GameDataColumnType.BOOLEAN),
		GameDataColumnSpec(name = "is_default", type = GameDataColumnType.BOOLEAN),
	),
	searchColumns = listOf("chain_id", "from_species_id", "to_species_id"),
)

/**
 * 进化条件持久化访问。
 */
@Repository
class GameEvolutionDetailsRepository(
	private val operations: GameDataJdbcOperations,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPage<GameDataRecordResponse> =
		operations.list(GAME_EVOLUTION_DETAILS_TABLE, page, size, query, filters)

	fun get(id: Long): GameDataRecordResponse =
		operations.get(GAME_EVOLUTION_DETAILS_TABLE, id)

	fun create(request: GameDataRecordRequest): GameDataRecordResponse =
		operations.create(GAME_EVOLUTION_DETAILS_TABLE, request)

	fun update(id: Long, request: GameDataRecordRequest): GameDataRecordResponse =
		operations.update(GAME_EVOLUTION_DETAILS_TABLE, id, request)

	fun delete(id: Long) {
		operations.delete(GAME_EVOLUTION_DETAILS_TABLE, id)
	}
}
