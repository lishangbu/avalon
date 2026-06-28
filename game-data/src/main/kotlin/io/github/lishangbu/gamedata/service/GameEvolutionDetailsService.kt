package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameEvolutionDetailsRequest
import io.github.lishangbu.gamedata.dto.GameEvolutionDetailsResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameEvolutionDetailsRepository
import org.springframework.stereotype.Service

/**
 * 进化条件 Service。
 */
@Service
class GameEvolutionDetailsService(
	private val repository: GameEvolutionDetailsRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameEvolutionDetailsResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameEvolutionDetailsResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameEvolutionDetailsResponse =
		GameEvolutionDetailsResponse.from(repository.get(id))

	fun create(request: GameEvolutionDetailsRequest): GameEvolutionDetailsResponse =
		GameEvolutionDetailsResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameEvolutionDetailsRequest): GameEvolutionDetailsResponse =
		GameEvolutionDetailsResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameEvolutionDetailsRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
