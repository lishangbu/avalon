package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameSpeciesRequest
import io.github.lishangbu.gamedata.dto.GameSpeciesResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameSpeciesRepository
import org.springframework.stereotype.Service

/**
 * 种类资料 Service。
 */
@Service
class GameSpeciesService(
	private val repository: GameSpeciesRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameSpeciesResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameSpeciesResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameSpeciesResponse =
		GameSpeciesResponse.from(repository.get(id))

	fun create(request: GameSpeciesRequest): GameSpeciesResponse =
		GameSpeciesResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameSpeciesRequest): GameSpeciesResponse =
		GameSpeciesResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameSpeciesRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
