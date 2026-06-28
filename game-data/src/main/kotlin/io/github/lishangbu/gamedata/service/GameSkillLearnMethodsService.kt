package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameSkillLearnMethodsRequest
import io.github.lishangbu.gamedata.dto.GameSkillLearnMethodsResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameSkillLearnMethodsRepository
import org.springframework.stereotype.Service

/**
 * 技能学习方式 Service。
 */
@Service
class GameSkillLearnMethodsService(
	private val repository: GameSkillLearnMethodsRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameSkillLearnMethodsResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameSkillLearnMethodsResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameSkillLearnMethodsResponse =
		GameSkillLearnMethodsResponse.from(repository.get(id))

	fun create(request: GameSkillLearnMethodsRequest): GameSkillLearnMethodsResponse =
		GameSkillLearnMethodsResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameSkillLearnMethodsRequest): GameSkillLearnMethodsResponse =
		GameSkillLearnMethodsResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameSkillLearnMethodsRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
