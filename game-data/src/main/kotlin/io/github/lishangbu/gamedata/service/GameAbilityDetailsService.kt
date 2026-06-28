package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameAbilityDetailsRequest
import io.github.lishangbu.gamedata.dto.GameAbilityDetailsResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameAbilityDetailsRepository
import org.springframework.stereotype.Service

/**
 * 特性详情 Service。
 */
@Service
class GameAbilityDetailsService(
	private val repository: GameAbilityDetailsRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameAbilityDetailsResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameAbilityDetailsResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameAbilityDetailsResponse =
		GameAbilityDetailsResponse.from(repository.get(id))

	fun create(request: GameAbilityDetailsRequest): GameAbilityDetailsResponse =
		GameAbilityDetailsResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameAbilityDetailsRequest): GameAbilityDetailsResponse =
		GameAbilityDetailsResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameAbilityDetailsRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
