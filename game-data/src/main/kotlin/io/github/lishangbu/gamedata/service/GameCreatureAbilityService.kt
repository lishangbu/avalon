package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameCreatureAbilityRequest
import io.github.lishangbu.gamedata.dto.GameCreatureAbilityResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameCreatureAbilityRepository
import org.springframework.stereotype.Service

/**
 * 生物特性绑定 Service。
 */
@Service
class GameCreatureAbilityService(
	private val repository: GameCreatureAbilityRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameCreatureAbilityResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameCreatureAbilityResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameCreatureAbilityResponse =
		GameCreatureAbilityResponse.from(repository.get(id))

	fun create(request: GameCreatureAbilityRequest): GameCreatureAbilityResponse =
		GameCreatureAbilityResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameCreatureAbilityRequest): GameCreatureAbilityResponse =
		GameCreatureAbilityResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameCreatureAbilityRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
