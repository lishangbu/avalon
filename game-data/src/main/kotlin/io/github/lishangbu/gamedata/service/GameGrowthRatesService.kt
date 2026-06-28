package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameGrowthRatesRequest
import io.github.lishangbu.gamedata.dto.GameGrowthRatesResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameGrowthRatesRepository
import org.springframework.stereotype.Service

/**
 * 成长速率 Service。
 */
@Service
class GameGrowthRatesService(
	private val repository: GameGrowthRatesRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameGrowthRatesResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameGrowthRatesResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameGrowthRatesResponse =
		GameGrowthRatesResponse.from(repository.get(id))

	fun create(request: GameGrowthRatesRequest): GameGrowthRatesResponse =
		GameGrowthRatesResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameGrowthRatesRequest): GameGrowthRatesResponse =
		GameGrowthRatesResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameGrowthRatesRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
