package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameAdvancedContestEffectsRequest
import io.github.lishangbu.gamedata.dto.GameAdvancedContestEffectsResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameAdvancedContestEffectsRepository
import org.springframework.stereotype.Service

/**
 * 高级评价效果 Service。
 */
@Service
class GameAdvancedContestEffectsService(
	private val repository: GameAdvancedContestEffectsRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameAdvancedContestEffectsResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameAdvancedContestEffectsResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameAdvancedContestEffectsResponse =
		GameAdvancedContestEffectsResponse.from(repository.get(id))

	fun create(request: GameAdvancedContestEffectsRequest): GameAdvancedContestEffectsResponse =
		GameAdvancedContestEffectsResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameAdvancedContestEffectsRequest): GameAdvancedContestEffectsResponse =
		GameAdvancedContestEffectsResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameAdvancedContestEffectsRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
