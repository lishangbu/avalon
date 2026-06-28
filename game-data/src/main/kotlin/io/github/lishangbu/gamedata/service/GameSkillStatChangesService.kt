package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameSkillStatChangesRequest
import io.github.lishangbu.gamedata.dto.GameSkillStatChangesResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameSkillStatChangesRepository
import org.springframework.stereotype.Service

/**
 * 技能数值变化 Service。
 */
@Service
class GameSkillStatChangesService(
	private val repository: GameSkillStatChangesRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameSkillStatChangesResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameSkillStatChangesResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameSkillStatChangesResponse =
		GameSkillStatChangesResponse.from(repository.get(id))

	fun create(request: GameSkillStatChangesRequest): GameSkillStatChangesResponse =
		GameSkillStatChangesResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameSkillStatChangesRequest): GameSkillStatChangesResponse =
		GameSkillStatChangesResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameSkillStatChangesRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
