package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameSpeciesColorRequest
import io.github.lishangbu.gamedata.dto.GameSpeciesColorResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameSpeciesColorRepository
import org.springframework.stereotype.Service

/**
 * 种类颜色 Service。
 */
@Service
class GameSpeciesColorService(
	private val repository: GameSpeciesColorRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameSpeciesColorResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameSpeciesColorResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameSpeciesColorResponse =
		GameSpeciesColorResponse.from(repository.get(id))

	fun create(request: GameSpeciesColorRequest): GameSpeciesColorResponse =
		GameSpeciesColorResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameSpeciesColorRequest): GameSpeciesColorResponse =
		GameSpeciesColorResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameSpeciesColorRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
