package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameCharacteristicsRequest
import io.github.lishangbu.gamedata.dto.GameCharacteristicsResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameCharacteristicsRepository
import org.springframework.stereotype.Service

/**
 * 个体特征 Service。
 */
@Service
class GameCharacteristicsService(
	private val repository: GameCharacteristicsRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameCharacteristicsResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameCharacteristicsResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameCharacteristicsResponse =
		GameCharacteristicsResponse.from(repository.get(id))

	fun create(request: GameCharacteristicsRequest): GameCharacteristicsResponse =
		GameCharacteristicsResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameCharacteristicsRequest): GameCharacteristicsResponse =
		GameCharacteristicsResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameCharacteristicsRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
