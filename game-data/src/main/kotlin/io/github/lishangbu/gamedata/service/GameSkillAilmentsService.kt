package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameSkillAilmentsRequest
import io.github.lishangbu.gamedata.dto.GameSkillAilmentsResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameSkillAilmentsRepository
import org.springframework.stereotype.Service

/**
 * 技能异常 Service。
 */
@Service
class GameSkillAilmentsService(
	private val repository: GameSkillAilmentsRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameSkillAilmentsResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameSkillAilmentsResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameSkillAilmentsResponse =
		GameSkillAilmentsResponse.from(repository.get(id))

	fun create(request: GameSkillAilmentsRequest): GameSkillAilmentsResponse =
		GameSkillAilmentsResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameSkillAilmentsRequest): GameSkillAilmentsResponse =
		GameSkillAilmentsResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameSkillAilmentsRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
