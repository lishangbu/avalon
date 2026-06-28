package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameSkillDetailsRequest
import io.github.lishangbu.gamedata.dto.GameSkillDetailsResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameSkillDetailsRepository
import org.springframework.stereotype.Service

/**
 * 技能详情 Service。
 */
@Service
class GameSkillDetailsService(
	private val repository: GameSkillDetailsRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameSkillDetailsResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameSkillDetailsResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameSkillDetailsResponse =
		GameSkillDetailsResponse.from(repository.get(id))

	fun create(request: GameSkillDetailsRequest): GameSkillDetailsResponse =
		GameSkillDetailsResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameSkillDetailsRequest): GameSkillDetailsResponse =
		GameSkillDetailsResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameSkillDetailsRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
