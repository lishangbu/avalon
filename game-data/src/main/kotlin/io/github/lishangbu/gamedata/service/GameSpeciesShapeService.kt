package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameSpeciesShapeRequest
import io.github.lishangbu.gamedata.dto.GameSpeciesShapeResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameSpeciesShapeRepository
import org.springframework.stereotype.Service

/**
 * 种类形态 Service。
 */
@Service
class GameSpeciesShapeService(
	private val repository: GameSpeciesShapeRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameSpeciesShapeResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameSpeciesShapeResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameSpeciesShapeResponse =
		GameSpeciesShapeResponse.from(repository.get(id))

	fun create(request: GameSpeciesShapeRequest): GameSpeciesShapeResponse =
		GameSpeciesShapeResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameSpeciesShapeRequest): GameSpeciesShapeResponse =
		GameSpeciesShapeResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameSpeciesShapeRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
