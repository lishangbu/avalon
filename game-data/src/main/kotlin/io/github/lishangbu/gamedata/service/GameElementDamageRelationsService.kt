package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameElementDamageRelationsRequest
import io.github.lishangbu.gamedata.dto.GameElementDamageRelationsResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameElementDamageRelationsRepository
import org.springframework.stereotype.Service

/**
 * 属性克制关系 Service。
 */
@Service
class GameElementDamageRelationsService(
	private val repository: GameElementDamageRelationsRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameElementDamageRelationsResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameElementDamageRelationsResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameElementDamageRelationsResponse =
		GameElementDamageRelationsResponse.from(repository.get(id))

	fun create(request: GameElementDamageRelationsRequest): GameElementDamageRelationsResponse =
		GameElementDamageRelationsResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameElementDamageRelationsRequest): GameElementDamageRelationsResponse =
		GameElementDamageRelationsResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameElementDamageRelationsRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
