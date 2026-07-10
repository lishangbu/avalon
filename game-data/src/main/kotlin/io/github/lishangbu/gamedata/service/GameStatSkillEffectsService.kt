package io.github.lishangbu.gamedata.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.dto.GameStatSkillEffectsRequest
import io.github.lishangbu.gamedata.dto.GameStatSkillEffectsResponse
import io.github.lishangbu.gamedata.entity.GameStatSkillEffects
import io.github.lishangbu.gamedata.entity.changeValue
import io.github.lishangbu.gamedata.entity.effectType
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.entity.skillId
import io.github.lishangbu.gamedata.entity.statId
import io.github.lishangbu.gamedata.repository.GameStatSkillEffectsRepository
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
 * 数值项技能影响维护服务。
 *
 * 查询、校验和写入只依赖当前资料实体，不共享跨表运行时元数据。
 */
@Service
class GameStatSkillEffectsService(
	private val repository: GameStatSkillEffectsRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): Page<GameStatSkillEffectsResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(GameStatSkillEffects::class) {
			search.pattern?.let { pattern ->
				where(or(sql<String>("cast(%e as text)", table.statId) ilike pattern, sql<String>("cast(%e as text)", table.skillId) ilike pattern))
			}
			filters.forEach { (field, rawValue) ->
				when (field) {
				"stat_id" -> gameDataLongFilterValue("stat_id", rawValue)?.let { where(table.statId eq it) }
				"skill_id" -> gameDataLongFilterValue("skill_id", rawValue)?.let { where(table.skillId eq it) }
				"change_value" -> gameDataIntFilterValue("change_value", rawValue)?.let { where(table.changeValue eq it) }
				"effect_type" -> gameDataStringFilterValue("effect_type", rawValue)?.let { where(table.effectType eq it) }
					else -> invalidValue(field, "筛选字段不存在: $field")
				}
			}
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): GameStatSkillEffectsResponse =
		entityByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: GameStatSkillEffectsRequest): GameStatSkillEffectsResponse =
		repository.save(
			GameStatSkillEffects {
				statId = request.statId ?: invalidValue("stat_id", "stat_id 不能为空")
				skillId = request.skillId ?: invalidValue("skill_id", "skill_id 不能为空")
				changeValue = request.changeValue ?: invalidValue("change_value", "change_value 不能为空")
				effectType = gameDataRequiredText(request.effectType, "effect_type", 20)
			},
			SaveMode.INSERT_ONLY,
		).toResponse()

	@Transactional
	fun update(id: Long, request: GameStatSkillEffectsRequest): GameStatSkillEffectsResponse {
		entityByIdOrNotFound(id)
		return repository.save(
			GameStatSkillEffects {
				this.id = id
				statId = request.statId ?: invalidValue("stat_id", "stat_id 不能为空")
				skillId = request.skillId ?: invalidValue("skill_id", "skill_id 不能为空")
				changeValue = request.changeValue ?: invalidValue("change_value", "change_value 不能为空")
				effectType = gameDataRequiredText(request.effectType, "effect_type", 20)
			},
			SaveMode.UPDATE_ONLY,
		).toResponse()
	}

	@Transactional
	fun delete(id: Long) {
		entityByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun entityByIdOrNotFound(id: Long): GameStatSkillEffects =
		repository.findNullable(id) ?: notFound("id", "数值项技能影响不存在: $id")

	private fun GameStatSkillEffects.toResponse(): GameStatSkillEffectsResponse =
		GameStatSkillEffectsResponse {
			id = this@toResponse.id
			statId = this@toResponse.statId
			skillId = this@toResponse.skillId
			changeValue = this@toResponse.changeValue
			effectType = this@toResponse.effectType
		}
}
