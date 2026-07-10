package io.github.lishangbu.gamedata.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.dto.GameSkillStatChangesRequest
import io.github.lishangbu.gamedata.dto.GameSkillStatChangesResponse
import io.github.lishangbu.gamedata.entity.GameSkillStatChanges
import io.github.lishangbu.gamedata.entity.changeValue
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.entity.skillId
import io.github.lishangbu.gamedata.entity.statId
import io.github.lishangbu.gamedata.repository.GameSkillStatChangesRepository
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
 * 技能数值变化维护服务。
 *
 * 查询、校验和写入只依赖当前资料实体，不共享跨表运行时元数据。
 */
@Service
class GameSkillStatChangesService(
	private val repository: GameSkillStatChangesRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): Page<GameSkillStatChangesResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(GameSkillStatChanges::class) {
			search.pattern?.let { pattern ->
				where(or(sql<String>("cast(%e as text)", table.skillId) ilike pattern, sql<String>("cast(%e as text)", table.statId) ilike pattern))
			}
			filters.forEach { (field, rawValue) ->
				when (field) {
				"skill_id" -> gameDataLongFilterValue("skill_id", rawValue)?.let { where(table.skillId eq it) }
				"stat_id" -> gameDataLongFilterValue("stat_id", rawValue)?.let { where(table.statId eq it) }
				"change_value" -> gameDataIntFilterValue("change_value", rawValue)?.let { where(table.changeValue eq it) }
					else -> invalidValue(field, "筛选字段不存在: $field")
				}
			}
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): GameSkillStatChangesResponse =
		entityByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: GameSkillStatChangesRequest): GameSkillStatChangesResponse =
		repository.save(
			GameSkillStatChanges {
				skillId = request.skillId ?: invalidValue("skill_id", "skill_id 不能为空")
				statId = request.statId ?: invalidValue("stat_id", "stat_id 不能为空")
				changeValue = request.changeValue ?: invalidValue("change_value", "change_value 不能为空")
			},
			SaveMode.INSERT_ONLY,
		).toResponse()

	@Transactional
	fun update(id: Long, request: GameSkillStatChangesRequest): GameSkillStatChangesResponse {
		entityByIdOrNotFound(id)
		return repository.save(
			GameSkillStatChanges {
				this.id = id
				skillId = request.skillId ?: invalidValue("skill_id", "skill_id 不能为空")
				statId = request.statId ?: invalidValue("stat_id", "stat_id 不能为空")
				changeValue = request.changeValue ?: invalidValue("change_value", "change_value 不能为空")
			},
			SaveMode.UPDATE_ONLY,
		).toResponse()
	}

	@Transactional
	fun delete(id: Long) {
		entityByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun entityByIdOrNotFound(id: Long): GameSkillStatChanges =
		repository.findNullable(id) ?: notFound("id", "技能数值变化不存在: $id")

	private fun GameSkillStatChanges.toResponse(): GameSkillStatChangesResponse =
		GameSkillStatChangesResponse {
			id = this@toResponse.id
			skillId = this@toResponse.skillId
			statId = this@toResponse.statId
			changeValue = this@toResponse.changeValue
		}
}
