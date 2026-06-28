package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameContestEffectsRequest
import io.github.lishangbu.gamedata.dto.GameContestEffectsResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameContestEffectsRepository
import org.springframework.stereotype.Service

/**
 * 评价效果 Service。
 */
@Service
class GameContestEffectsService(
	private val repository: GameContestEffectsRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameContestEffectsResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameContestEffectsResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameContestEffectsResponse =
		GameContestEffectsResponse.from(repository.get(id))

	fun create(request: GameContestEffectsRequest): GameContestEffectsResponse =
		GameContestEffectsResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameContestEffectsRequest): GameContestEffectsResponse =
		GameContestEffectsResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameContestEffectsRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
