package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameLocationsRequest
import io.github.lishangbu.gamedata.dto.GameLocationsResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameLocationsRepository
import org.springframework.stereotype.Service

/**
 * 地点资料 Service。
 */
@Service
class GameLocationsService(
	private val repository: GameLocationsRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameLocationsResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameLocationsResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameLocationsResponse =
		GameLocationsResponse.from(repository.get(id))

	fun create(request: GameLocationsRequest): GameLocationsResponse =
		GameLocationsResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameLocationsRequest): GameLocationsResponse =
		GameLocationsResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameLocationsRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
