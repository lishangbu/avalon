package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameLocationAreaMethodRatesRequest
import io.github.lishangbu.gamedata.dto.GameLocationAreaMethodRatesResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameLocationAreaMethodRatesRepository
import org.springframework.stereotype.Service

/**
 * 区域遭遇方式概率 Service。
 */
@Service
class GameLocationAreaMethodRatesService(
	private val repository: GameLocationAreaMethodRatesRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameLocationAreaMethodRatesResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameLocationAreaMethodRatesResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameLocationAreaMethodRatesResponse =
		GameLocationAreaMethodRatesResponse.from(repository.get(id))

	fun create(request: GameLocationAreaMethodRatesRequest): GameLocationAreaMethodRatesResponse =
		GameLocationAreaMethodRatesResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameLocationAreaMethodRatesRequest): GameLocationAreaMethodRatesResponse =
		GameLocationAreaMethodRatesResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameLocationAreaMethodRatesRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
