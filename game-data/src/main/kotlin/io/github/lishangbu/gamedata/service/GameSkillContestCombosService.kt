package io.github.lishangbu.gamedata.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.dto.GameSkillContestCombosRequest
import io.github.lishangbu.gamedata.dto.GameSkillContestCombosResponse
import io.github.lishangbu.gamedata.entity.GameSkillContestCombos
import io.github.lishangbu.gamedata.entity.comboType
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.entity.relatedSkillId
import io.github.lishangbu.gamedata.entity.relationType
import io.github.lishangbu.gamedata.entity.skillId
import io.github.lishangbu.gamedata.repository.GameSkillContestCombosRepository
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
 * 技能评价组合维护服务。
 *
 * 查询、校验和写入只依赖当前资料实体，不共享跨表运行时元数据。
 */
@Service
class GameSkillContestCombosService(
	private val repository: GameSkillContestCombosRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): Page<GameSkillContestCombosResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(GameSkillContestCombos::class) {
			search.pattern?.let { pattern ->
				where(or(sql<String>("cast(%e as text)", table.skillId) ilike pattern, sql<String>("cast(%e as text)", table.relatedSkillId) ilike pattern))
			}
			filters.forEach { (field, rawValue) ->
				when (field) {
				"skill_id" -> gameDataLongFilterValue("skill_id", rawValue)?.let { where(table.skillId eq it) }
				"combo_type" -> gameDataStringFilterValue("combo_type", rawValue)?.let { where(table.comboType eq it) }
				"relation_type" -> gameDataStringFilterValue("relation_type", rawValue)?.let { where(table.relationType eq it) }
				"related_skill_id" -> gameDataLongFilterValue("related_skill_id", rawValue)?.let { where(table.relatedSkillId eq it) }
					else -> invalidValue(field, "筛选字段不存在: $field")
				}
			}
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): GameSkillContestCombosResponse =
		entityByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: GameSkillContestCombosRequest): GameSkillContestCombosResponse =
		repository.save(
			GameSkillContestCombos {
				skillId = request.skillId ?: invalidValue("skill_id", "skill_id 不能为空")
				comboType = gameDataRequiredText(request.comboType, "combo_type", 40)
				relationType = gameDataRequiredText(request.relationType, "relation_type", 40)
				relatedSkillId = request.relatedSkillId ?: invalidValue("related_skill_id", "related_skill_id 不能为空")
			},
			SaveMode.INSERT_ONLY,
		).toResponse()

	@Transactional
	fun update(id: Long, request: GameSkillContestCombosRequest): GameSkillContestCombosResponse {
		entityByIdOrNotFound(id)
		return repository.save(
			GameSkillContestCombos {
				this.id = id
				skillId = request.skillId ?: invalidValue("skill_id", "skill_id 不能为空")
				comboType = gameDataRequiredText(request.comboType, "combo_type", 40)
				relationType = gameDataRequiredText(request.relationType, "relation_type", 40)
				relatedSkillId = request.relatedSkillId ?: invalidValue("related_skill_id", "related_skill_id 不能为空")
			},
			SaveMode.UPDATE_ONLY,
		).toResponse()
	}

	@Transactional
	fun delete(id: Long) {
		entityByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun entityByIdOrNotFound(id: Long): GameSkillContestCombos =
		repository.findNullable(id) ?: notFound("id", "技能评价组合不存在: $id")

	private fun GameSkillContestCombos.toResponse(): GameSkillContestCombosResponse =
		GameSkillContestCombosResponse {
			id = this@toResponse.id
			skillId = this@toResponse.skillId
			comboType = this@toResponse.comboType
			relationType = this@toResponse.relationType
			relatedSkillId = this@toResponse.relatedSkillId
		}
}
