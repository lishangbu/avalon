package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameEventStatNatureEffectsRequest
import io.github.lishangbu.gamedata.dto.GameEventStatNatureEffectsResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameEventStatNatureEffectsRepository
import org.springframework.stereotype.Service

/**
 * 活动能力性格影响 Service。
 */
@Service
class GameEventStatNatureEffectsService(
	private val repository: GameEventStatNatureEffectsRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameEventStatNatureEffectsResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameEventStatNatureEffectsResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameEventStatNatureEffectsResponse =
		GameEventStatNatureEffectsResponse.from(repository.get(id))

	fun create(request: GameEventStatNatureEffectsRequest): GameEventStatNatureEffectsResponse =
		GameEventStatNatureEffectsResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameEventStatNatureEffectsRequest): GameEventStatNatureEffectsResponse =
		GameEventStatNatureEffectsResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameEventStatNatureEffectsRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
