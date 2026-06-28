package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameStatNatureEffectsRequest
import io.github.lishangbu.gamedata.dto.GameStatNatureEffectsResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameStatNatureEffectsRepository
import org.springframework.stereotype.Service

/**
 * 数值项性格影响 Service。
 */
@Service
class GameStatNatureEffectsService(
	private val repository: GameStatNatureEffectsRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameStatNatureEffectsResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameStatNatureEffectsResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameStatNatureEffectsResponse =
		GameStatNatureEffectsResponse.from(repository.get(id))

	fun create(request: GameStatNatureEffectsRequest): GameStatNatureEffectsResponse =
		GameStatNatureEffectsResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameStatNatureEffectsRequest): GameStatNatureEffectsResponse =
		GameStatNatureEffectsResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameStatNatureEffectsRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
