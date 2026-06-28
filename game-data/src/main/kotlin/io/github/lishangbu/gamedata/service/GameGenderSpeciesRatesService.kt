package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameGenderSpeciesRatesRequest
import io.github.lishangbu.gamedata.dto.GameGenderSpeciesRatesResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameGenderSpeciesRatesRepository
import org.springframework.stereotype.Service

/**
 * 性别种类比例 Service。
 */
@Service
class GameGenderSpeciesRatesService(
	private val repository: GameGenderSpeciesRatesRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameGenderSpeciesRatesResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameGenderSpeciesRatesResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameGenderSpeciesRatesResponse =
		GameGenderSpeciesRatesResponse.from(repository.get(id))

	fun create(request: GameGenderSpeciesRatesRequest): GameGenderSpeciesRatesResponse =
		GameGenderSpeciesRatesResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameGenderSpeciesRatesRequest): GameGenderSpeciesRatesResponse =
		GameGenderSpeciesRatesResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameGenderSpeciesRatesRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
