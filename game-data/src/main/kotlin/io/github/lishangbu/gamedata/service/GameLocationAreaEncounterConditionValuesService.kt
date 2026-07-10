package io.github.lishangbu.gamedata.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.dto.GameLocationAreaEncounterConditionValuesRequest
import io.github.lishangbu.gamedata.dto.GameLocationAreaEncounterConditionValuesResponse
import io.github.lishangbu.gamedata.entity.GameLocationAreaEncounterConditionValues
import io.github.lishangbu.gamedata.entity.conditionValueId
import io.github.lishangbu.gamedata.entity.encounterId
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.repository.GameLocationAreaEncounterConditionValuesRepository
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
 * 区域遭遇条件绑定维护服务。
 *
 * 查询、校验和写入只依赖当前资料实体，不共享跨表运行时元数据。
 */
@Service
class GameLocationAreaEncounterConditionValuesService(
	private val repository: GameLocationAreaEncounterConditionValuesRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): Page<GameLocationAreaEncounterConditionValuesResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(GameLocationAreaEncounterConditionValues::class) {
			search.pattern?.let { pattern ->
				where(or(sql<String>("cast(%e as text)", table.encounterId) ilike pattern, sql<String>("cast(%e as text)", table.conditionValueId) ilike pattern))
			}
			filters.forEach { (field, rawValue) ->
				when (field) {
				"encounter_id" -> gameDataLongFilterValue("encounter_id", rawValue)?.let { where(table.encounterId eq it) }
				"condition_value_id" -> gameDataLongFilterValue("condition_value_id", rawValue)?.let { where(table.conditionValueId eq it) }
					else -> invalidValue(field, "筛选字段不存在: $field")
				}
			}
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): GameLocationAreaEncounterConditionValuesResponse =
		entityByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: GameLocationAreaEncounterConditionValuesRequest): GameLocationAreaEncounterConditionValuesResponse =
		repository.save(
			GameLocationAreaEncounterConditionValues {
				encounterId = request.encounterId ?: invalidValue("encounter_id", "encounter_id 不能为空")
				conditionValueId = request.conditionValueId ?: invalidValue("condition_value_id", "condition_value_id 不能为空")
			},
			SaveMode.INSERT_ONLY,
		).toResponse()

	@Transactional
	fun update(id: Long, request: GameLocationAreaEncounterConditionValuesRequest): GameLocationAreaEncounterConditionValuesResponse {
		entityByIdOrNotFound(id)
		return repository.save(
			GameLocationAreaEncounterConditionValues {
				this.id = id
				encounterId = request.encounterId ?: invalidValue("encounter_id", "encounter_id 不能为空")
				conditionValueId = request.conditionValueId ?: invalidValue("condition_value_id", "condition_value_id 不能为空")
			},
			SaveMode.UPDATE_ONLY,
		).toResponse()
	}

	@Transactional
	fun delete(id: Long) {
		entityByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun entityByIdOrNotFound(id: Long): GameLocationAreaEncounterConditionValues =
		repository.findNullable(id) ?: notFound("id", "区域遭遇条件绑定不存在: $id")

	private fun GameLocationAreaEncounterConditionValues.toResponse(): GameLocationAreaEncounterConditionValuesResponse =
		GameLocationAreaEncounterConditionValuesResponse {
			id = this@toResponse.id
			encounterId = this@toResponse.encounterId
			conditionValueId = this@toResponse.conditionValueId
		}
}
