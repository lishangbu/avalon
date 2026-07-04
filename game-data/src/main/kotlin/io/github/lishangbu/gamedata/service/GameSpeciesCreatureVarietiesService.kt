package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameSpeciesCreatureVarietiesRequest
import io.github.lishangbu.gamedata.dto.GameSpeciesCreatureVarietiesResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameSpeciesCreatureVarietiesRepository
import org.springframework.stereotype.Service

/**
 * 种类精灵变种 Service。
 */
@Service
class GameSpeciesCreatureVarietiesService(
	private val repository: GameSpeciesCreatureVarietiesRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameSpeciesCreatureVarietiesResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameSpeciesCreatureVarietiesResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameSpeciesCreatureVarietiesResponse =
		GameSpeciesCreatureVarietiesResponse.from(repository.get(id))

	fun create(request: GameSpeciesCreatureVarietiesRequest): GameSpeciesCreatureVarietiesResponse =
		GameSpeciesCreatureVarietiesResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameSpeciesCreatureVarietiesRequest): GameSpeciesCreatureVarietiesResponse =
		GameSpeciesCreatureVarietiesResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameSpeciesCreatureVarietiesRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
