package io.github.lishangbu.gamedata.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.dto.GameCreatureSkillLearnsRequest
import io.github.lishangbu.gamedata.dto.GameCreatureSkillLearnsResponse
import io.github.lishangbu.gamedata.entity.GameCreatureSkillLearns
import io.github.lishangbu.gamedata.entity.creatureId
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.entity.learnMethodId
import io.github.lishangbu.gamedata.entity.levelLearnedAt
import io.github.lishangbu.gamedata.entity.skillId
import io.github.lishangbu.gamedata.repository.GameCreatureSkillLearnsRepository
import io.github.lishangbu.gamedata.support.gameDataBooleanFilterValue
import io.github.lishangbu.gamedata.support.gameDataIntFilterValue
import io.github.lishangbu.gamedata.support.gameDataLongFilterValue
import io.github.lishangbu.gamedata.support.gameDataOptionalText
import io.github.lishangbu.gamedata.support.gameDataRequiredText
import io.github.lishangbu.gamedata.support.gameDataStringFilterValue
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.ilike
import org.babyfish.jimmer.sql.kt.ast.expression.or
import org.babyfish.jimmer.sql.kt.ast.expression.sql
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 精灵技能学习维护服务。
 *
 * 查询、校验和写入只依赖当前资料实体，不共享跨表运行时元数据。
 */
@Service
class GameCreatureSkillLearnsService(
	private val repository: GameCreatureSkillLearnsRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): Page<GameCreatureSkillLearnsResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(GameCreatureSkillLearns::class) {
			search.pattern?.let { pattern ->
				where(or(sql<String>("cast(%e as text)", table.creatureId) ilike pattern, sql<String>("cast(%e as text)", table.skillId) ilike pattern))
			}
			filters.forEach { (field, rawValue) ->
				when (field) {
				"creature_id" -> gameDataLongFilterValue("creature_id", rawValue)?.let { where(table.creatureId eq it) }
				"skill_id" -> gameDataLongFilterValue("skill_id", rawValue)?.let { where(table.skillId eq it) }
				"learn_method_id" -> gameDataLongFilterValue("learn_method_id", rawValue)?.let { where(table.learnMethodId eq it) }
				"level_learned_at" -> gameDataIntFilterValue("level_learned_at", rawValue)?.let { where(table.levelLearnedAt eq it) }
					else -> invalidValue(field, "筛选字段不存在: $field")
				}
			}
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): GameCreatureSkillLearnsResponse =
		entityByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: GameCreatureSkillLearnsRequest): GameCreatureSkillLearnsResponse =
		repository.save(
			GameCreatureSkillLearns {
				creatureId = request.creatureId ?: invalidValue("creature_id", "creature_id 不能为空")
				skillId = request.skillId ?: invalidValue("skill_id", "skill_id 不能为空")
				learnMethodId = request.learnMethodId ?: invalidValue("learn_method_id", "learn_method_id 不能为空")
				levelLearnedAt = request.levelLearnedAt ?: invalidValue("level_learned_at", "level_learned_at 不能为空")
			},
			SaveMode.INSERT_ONLY,
		).toResponse()

	@Transactional
	fun update(id: Long, request: GameCreatureSkillLearnsRequest): GameCreatureSkillLearnsResponse {
		entityByIdOrNotFound(id)
		return repository.save(
			GameCreatureSkillLearns {
				this.id = id
				creatureId = request.creatureId ?: invalidValue("creature_id", "creature_id 不能为空")
				skillId = request.skillId ?: invalidValue("skill_id", "skill_id 不能为空")
				learnMethodId = request.learnMethodId ?: invalidValue("learn_method_id", "learn_method_id 不能为空")
				levelLearnedAt = request.levelLearnedAt ?: invalidValue("level_learned_at", "level_learned_at 不能为空")
			},
			SaveMode.UPDATE_ONLY,
		).toResponse()
	}

	@Transactional
	fun delete(id: Long) {
		entityByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun entityByIdOrNotFound(id: Long): GameCreatureSkillLearns =
		repository.findNullable(id) ?: notFound("id", "精灵技能学习不存在: $id")

	private fun GameCreatureSkillLearns.toResponse(): GameCreatureSkillLearnsResponse =
		GameCreatureSkillLearnsResponse {
			id = this@toResponse.id
			creatureId = this@toResponse.creatureId
			skillId = this@toResponse.skillId
			learnMethodId = this@toResponse.learnMethodId
			levelLearnedAt = this@toResponse.levelLearnedAt
		}
}
