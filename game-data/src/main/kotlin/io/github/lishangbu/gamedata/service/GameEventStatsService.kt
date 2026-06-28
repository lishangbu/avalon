package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameEventStatsRequest
import io.github.lishangbu.gamedata.dto.GameEventStatsResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameEventStatsRepository
import org.springframework.stereotype.Service

/**
 * 活动能力项 Service。
 */
@Service
class GameEventStatsService(
	private val repository: GameEventStatsRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameEventStatsResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameEventStatsResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameEventStatsResponse =
		GameEventStatsResponse.from(repository.get(id))

	fun create(request: GameEventStatsRequest): GameEventStatsResponse =
		GameEventStatsResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameEventStatsRequest): GameEventStatsResponse =
		GameEventStatsResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameEventStatsRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
