package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameEggGroupRequest
import io.github.lishangbu.gamedata.dto.GameEggGroupResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameEggGroupRepository
import org.springframework.stereotype.Service

/**
 * 种类分组 Service。
 */
@Service
class GameEggGroupService(
	private val repository: GameEggGroupRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameEggGroupResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameEggGroupResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameEggGroupResponse =
		GameEggGroupResponse.from(repository.get(id))

	fun create(request: GameEggGroupRequest): GameEggGroupResponse =
		GameEggGroupResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameEggGroupRequest): GameEggGroupResponse =
		GameEggGroupResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameEggGroupRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
