package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameStatCharacteristicsRequest
import io.github.lishangbu.gamedata.dto.GameStatCharacteristicsResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameStatCharacteristicsRepository
import org.springframework.stereotype.Service

/**
 * 数值项特征 Service。
 */
@Service
class GameStatCharacteristicsService(
	private val repository: GameStatCharacteristicsRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameStatCharacteristicsResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameStatCharacteristicsResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameStatCharacteristicsResponse =
		GameStatCharacteristicsResponse.from(repository.get(id))

	fun create(request: GameStatCharacteristicsRequest): GameStatCharacteristicsResponse =
		GameStatCharacteristicsResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameStatCharacteristicsRequest): GameStatCharacteristicsResponse =
		GameStatCharacteristicsResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameStatCharacteristicsRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
