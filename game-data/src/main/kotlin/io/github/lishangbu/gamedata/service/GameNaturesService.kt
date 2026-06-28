package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameNaturesRequest
import io.github.lishangbu.gamedata.dto.GameNaturesResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameNaturesRepository
import org.springframework.stereotype.Service

/**
 * 性格资料 Service。
 */
@Service
class GameNaturesService(
	private val repository: GameNaturesRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameNaturesResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameNaturesResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameNaturesResponse =
		GameNaturesResponse.from(repository.get(id))

	fun create(request: GameNaturesRequest): GameNaturesResponse =
		GameNaturesResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameNaturesRequest): GameNaturesResponse =
		GameNaturesResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameNaturesRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
