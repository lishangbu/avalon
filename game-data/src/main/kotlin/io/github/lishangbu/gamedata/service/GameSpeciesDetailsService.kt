package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameSpeciesDetailsRequest
import io.github.lishangbu.gamedata.dto.GameSpeciesDetailsResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameSpeciesDetailsRepository
import org.springframework.stereotype.Service

/**
 * 种类详情 Service。
 */
@Service
class GameSpeciesDetailsService(
	private val repository: GameSpeciesDetailsRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameSpeciesDetailsResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameSpeciesDetailsResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameSpeciesDetailsResponse =
		GameSpeciesDetailsResponse.from(repository.get(id))

	fun create(request: GameSpeciesDetailsRequest): GameSpeciesDetailsResponse =
		GameSpeciesDetailsResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameSpeciesDetailsRequest): GameSpeciesDetailsResponse =
		GameSpeciesDetailsResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameSpeciesDetailsRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
