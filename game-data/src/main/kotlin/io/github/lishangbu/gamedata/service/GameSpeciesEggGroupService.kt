package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameSpeciesEggGroupRequest
import io.github.lishangbu.gamedata.dto.GameSpeciesEggGroupResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameSpeciesEggGroupRepository
import org.springframework.stereotype.Service

/**
 * 种类分组绑定 Service。
 */
@Service
class GameSpeciesEggGroupService(
	private val repository: GameSpeciesEggGroupRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameSpeciesEggGroupResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameSpeciesEggGroupResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameSpeciesEggGroupResponse =
		GameSpeciesEggGroupResponse.from(repository.get(id))

	fun create(request: GameSpeciesEggGroupRequest): GameSpeciesEggGroupResponse =
		GameSpeciesEggGroupResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameSpeciesEggGroupRequest): GameSpeciesEggGroupResponse =
		GameSpeciesEggGroupResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameSpeciesEggGroupRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
