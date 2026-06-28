package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameCreatureSkillLearnsRequest
import io.github.lishangbu.gamedata.dto.GameCreatureSkillLearnsResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameCreatureSkillLearnsRepository
import org.springframework.stereotype.Service

/**
 * 生物技能学习 Service。
 */
@Service
class GameCreatureSkillLearnsService(
	private val repository: GameCreatureSkillLearnsRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameCreatureSkillLearnsResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameCreatureSkillLearnsResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameCreatureSkillLearnsResponse =
		GameCreatureSkillLearnsResponse.from(repository.get(id))

	fun create(request: GameCreatureSkillLearnsRequest): GameCreatureSkillLearnsResponse =
		GameCreatureSkillLearnsResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameCreatureSkillLearnsRequest): GameCreatureSkillLearnsResponse =
		GameCreatureSkillLearnsResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameCreatureSkillLearnsRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
