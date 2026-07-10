package io.github.lishangbu.gamedata.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.dto.GameCreatureRequest
import io.github.lishangbu.gamedata.dto.GameCreatureResponse
import io.github.lishangbu.gamedata.entity.GameCreature
import io.github.lishangbu.gamedata.entity.baseExperience
import io.github.lishangbu.gamedata.entity.code
import io.github.lishangbu.gamedata.entity.defaultForm
import io.github.lishangbu.gamedata.entity.enabled
import io.github.lishangbu.gamedata.entity.height
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.entity.inheritsFromCreatureId
import io.github.lishangbu.gamedata.entity.name
import io.github.lishangbu.gamedata.entity.sortOrder
import io.github.lishangbu.gamedata.entity.speciesId
import io.github.lishangbu.gamedata.entity.weight
import io.github.lishangbu.gamedata.repository.GameCreatureRepository
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
 * 精灵资料维护服务。
 *
 * 查询、校验和写入只依赖当前资料实体，不共享跨表运行时元数据。
 */
@Service
class GameCreatureService(
	private val repository: GameCreatureRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): Page<GameCreatureResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(GameCreature::class) {
			search.pattern?.let { pattern ->
				where(or(table.code ilike pattern, table.name ilike pattern))
			}
			filters.forEach { (field, rawValue) ->
				when (field) {
				"code" -> gameDataStringFilterValue("code", rawValue)?.let { where(table.code eq it) }
				"name" -> gameDataStringFilterValue("name", rawValue)?.let { where(table.name eq it) }
				"species_id" -> gameDataLongFilterValue("species_id", rawValue)?.let { where(table.speciesId eq it) }
				"inherits_from_creature_id" -> gameDataLongFilterValue("inherits_from_creature_id", rawValue)?.let { where(table.inheritsFromCreatureId eq it) }
				"height" -> gameDataIntFilterValue("height", rawValue)?.let { where(table.height eq it) }
				"weight" -> gameDataIntFilterValue("weight", rawValue)?.let { where(table.weight eq it) }
				"base_experience" -> gameDataIntFilterValue("base_experience", rawValue)?.let { where(table.baseExperience eq it) }
				"sort_order" -> gameDataIntFilterValue("sort_order", rawValue)?.let { where(table.sortOrder eq it) }
				"default_form" -> gameDataBooleanFilterValue("default_form", rawValue)?.let { where(table.defaultForm eq it) }
				"enabled" -> gameDataBooleanFilterValue("enabled", rawValue)?.let { where(table.enabled eq it) }
					else -> invalidValue(field, "筛选字段不存在: $field")
				}
			}
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): GameCreatureResponse =
		entityByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: GameCreatureRequest): GameCreatureResponse =
		repository.save(
			GameCreature {
				code = request.code.orEmpty().requiredSlugCode("code")
				name = gameDataRequiredText(request.name, "name", 120)
				speciesId = request.speciesId ?: invalidValue("species_id", "species_id 不能为空")
				inheritsFromCreatureId = request.inheritsFromCreatureId
				height = request.height
				weight = request.weight
				baseExperience = request.baseExperience
				sortOrder = request.sortOrder
				defaultForm = request.defaultForm
				enabled = request.enabled
			},
			SaveMode.INSERT_ONLY,
		).toResponse()

	@Transactional
	fun update(id: Long, request: GameCreatureRequest): GameCreatureResponse {
		entityByIdOrNotFound(id)
		return repository.save(
			GameCreature {
				this.id = id
				code = request.code.orEmpty().requiredSlugCode("code")
				name = gameDataRequiredText(request.name, "name", 120)
				speciesId = request.speciesId ?: invalidValue("species_id", "species_id 不能为空")
				inheritsFromCreatureId = request.inheritsFromCreatureId
				height = request.height
				weight = request.weight
				baseExperience = request.baseExperience
				sortOrder = request.sortOrder
				defaultForm = request.defaultForm
				enabled = request.enabled
			},
			SaveMode.UPDATE_ONLY,
		).toResponse()
	}

	@Transactional
	fun delete(id: Long) {
		entityByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun entityByIdOrNotFound(id: Long): GameCreature =
		repository.findNullable(id) ?: notFound("id", "精灵资料不存在: $id")

	private fun GameCreature.toResponse(): GameCreatureResponse =
		GameCreatureResponse {
			id = this@toResponse.id
			code = this@toResponse.code
			name = this@toResponse.name
			speciesId = this@toResponse.speciesId
			inheritsFromCreatureId = this@toResponse.inheritsFromCreatureId
			height = this@toResponse.height
			weight = this@toResponse.weight
			baseExperience = this@toResponse.baseExperience
			sortOrder = this@toResponse.sortOrder
			defaultForm = this@toResponse.defaultForm
			enabled = this@toResponse.enabled
		}
}
