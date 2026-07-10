package io.github.lishangbu.gamedata.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.dto.GameCreatureFormsRequest
import io.github.lishangbu.gamedata.dto.GameCreatureFormsResponse
import io.github.lishangbu.gamedata.entity.GameCreatureForms
import io.github.lishangbu.gamedata.entity.battleOnly
import io.github.lishangbu.gamedata.entity.code
import io.github.lishangbu.gamedata.entity.creatureId
import io.github.lishangbu.gamedata.entity.defaultForm
import io.github.lishangbu.gamedata.entity.enabled
import io.github.lishangbu.gamedata.entity.enhancedForm
import io.github.lishangbu.gamedata.entity.formName
import io.github.lishangbu.gamedata.entity.formOrder
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.entity.name
import io.github.lishangbu.gamedata.entity.sortOrder
import io.github.lishangbu.gamedata.repository.GameCreatureFormsRepository
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
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 精灵形态维护服务。
 *
 * 查询、校验和写入只依赖当前资料实体，不共享跨表运行时元数据。
 */
@Service
class GameCreatureFormsService(
	private val repository: GameCreatureFormsRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): Page<GameCreatureFormsResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(GameCreatureForms::class) {
			search.pattern?.let { pattern ->
				where(or(table.code ilike pattern, table.name ilike pattern, table.formName ilike pattern))
			}
			filters.forEach { (field, rawValue) ->
				when (field) {
				"code" -> gameDataStringFilterValue("code", rawValue)?.let { where(table.code eq it) }
				"name" -> gameDataStringFilterValue("name", rawValue)?.let { where(table.name eq it) }
				"creature_id" -> gameDataLongFilterValue("creature_id", rawValue)?.let { where(table.creatureId eq it) }
				"form_name" -> gameDataStringFilterValue("form_name", rawValue)?.let { where(table.formName eq it) }
				"sort_order" -> gameDataIntFilterValue("sort_order", rawValue)?.let { where(table.sortOrder eq it) }
				"form_order" -> gameDataIntFilterValue("form_order", rawValue)?.let { where(table.formOrder eq it) }
				"battle_only" -> gameDataBooleanFilterValue("battle_only", rawValue)?.let { where(table.battleOnly eq it) }
				"default_form" -> gameDataBooleanFilterValue("default_form", rawValue)?.let { where(table.defaultForm eq it) }
				"enhanced_form" -> gameDataBooleanFilterValue("enhanced_form", rawValue)?.let { where(table.enhancedForm eq it) }
				"enabled" -> gameDataBooleanFilterValue("enabled", rawValue)?.let { where(table.enabled eq it) }
					else -> invalidValue(field, "筛选字段不存在: $field")
				}
			}
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): GameCreatureFormsResponse =
		entityByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: GameCreatureFormsRequest): GameCreatureFormsResponse =
		repository.save(
			GameCreatureForms {
				code = request.code.orEmpty().requiredSlugCode("code")
				name = gameDataRequiredText(request.name, "name", 120)
				creatureId = request.creatureId
				formName = gameDataOptionalText(request.formName, "form_name", 120)
				sortOrder = request.sortOrder
				formOrder = request.formOrder
				battleOnly = request.battleOnly ?: invalidValue("battle_only", "battle_only 不能为空")
				defaultForm = request.defaultForm ?: invalidValue("default_form", "default_form 不能为空")
				enhancedForm = request.enhancedForm ?: invalidValue("enhanced_form", "enhanced_form 不能为空")
				enabled = request.enabled ?: invalidValue("enabled", "enabled 不能为空")
			},
			SaveMode.INSERT_ONLY,
		).toResponse()

	@Transactional
	fun update(id: Long, request: GameCreatureFormsRequest): GameCreatureFormsResponse {
		entityByIdOrNotFound(id)
		return repository.save(
			GameCreatureForms {
				this.id = id
				code = request.code.orEmpty().requiredSlugCode("code")
				name = gameDataRequiredText(request.name, "name", 120)
				creatureId = request.creatureId
				formName = gameDataOptionalText(request.formName, "form_name", 120)
				sortOrder = request.sortOrder
				formOrder = request.formOrder
				battleOnly = request.battleOnly ?: invalidValue("battle_only", "battle_only 不能为空")
				defaultForm = request.defaultForm ?: invalidValue("default_form", "default_form 不能为空")
				enhancedForm = request.enhancedForm ?: invalidValue("enhanced_form", "enhanced_form 不能为空")
				enabled = request.enabled ?: invalidValue("enabled", "enabled 不能为空")
			},
			SaveMode.UPDATE_ONLY,
		).toResponse()
	}

	@Transactional
	fun delete(id: Long) {
		entityByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun entityByIdOrNotFound(id: Long): GameCreatureForms =
		repository.findNullable(id) ?: notFound("id", "精灵形态不存在: $id")

	private fun GameCreatureForms.toResponse(): GameCreatureFormsResponse =
		GameCreatureFormsResponse {
			id = this@toResponse.id
			code = this@toResponse.code
			name = this@toResponse.name
			creatureId = this@toResponse.creatureId
			formName = this@toResponse.formName
			sortOrder = this@toResponse.sortOrder
			formOrder = this@toResponse.formOrder
			battleOnly = this@toResponse.battleOnly
			defaultForm = this@toResponse.defaultForm
			enhancedForm = this@toResponse.enhancedForm
			enabled = this@toResponse.enabled
		}
}
