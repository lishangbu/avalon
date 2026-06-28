package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameAbilityRequest
import io.github.lishangbu.gamedata.dto.GameAbilityResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameAbilityRepository
import org.springframework.stereotype.Service

/**
 * 特性资料 Service。
 */
@Service
class GameAbilityService(
	private val repository: GameAbilityRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameAbilityResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameAbilityResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameAbilityResponse =
		GameAbilityResponse.from(repository.get(id))

	fun create(request: GameAbilityRequest): GameAbilityResponse =
		GameAbilityResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameAbilityRequest): GameAbilityResponse =
		GameAbilityResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameAbilityRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
