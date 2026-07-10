package io.github.lishangbu.gamedata.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.dto.GameSpeciesRequest
import io.github.lishangbu.gamedata.dto.GameSpeciesResponse
import io.github.lishangbu.gamedata.entity.GameSpecies
import io.github.lishangbu.gamedata.entity.baby
import io.github.lishangbu.gamedata.entity.baseHappiness
import io.github.lishangbu.gamedata.entity.captureRate
import io.github.lishangbu.gamedata.entity.code
import io.github.lishangbu.gamedata.entity.colorId
import io.github.lishangbu.gamedata.entity.enabled
import io.github.lishangbu.gamedata.entity.genderRate
import io.github.lishangbu.gamedata.entity.habitatId
import io.github.lishangbu.gamedata.entity.hatchCounter
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.entity.legendary
import io.github.lishangbu.gamedata.entity.mythical
import io.github.lishangbu.gamedata.entity.name
import io.github.lishangbu.gamedata.entity.shapeId
import io.github.lishangbu.gamedata.repository.GameSpeciesRepository
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
 * 种类资料维护服务。
 *
 * 查询、校验和写入只依赖当前资料实体，不共享跨表运行时元数据。
 */
@Service
class GameSpeciesService(
	private val repository: GameSpeciesRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): Page<GameSpeciesResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(GameSpecies::class) {
			search.pattern?.let { pattern ->
				where(or(table.code ilike pattern, table.name ilike pattern))
			}
			filters.forEach { (field, rawValue) ->
				when (field) {
				"code" -> gameDataStringFilterValue("code", rawValue)?.let { where(table.code eq it) }
				"name" -> gameDataStringFilterValue("name", rawValue)?.let { where(table.name eq it) }
				"color_id" -> gameDataLongFilterValue("color_id", rawValue)?.let { where(table.colorId eq it) }
				"shape_id" -> gameDataLongFilterValue("shape_id", rawValue)?.let { where(table.shapeId eq it) }
				"habitat_id" -> gameDataLongFilterValue("habitat_id", rawValue)?.let { where(table.habitatId eq it) }
				"gender_rate" -> gameDataIntFilterValue("gender_rate", rawValue)?.let { where(table.genderRate eq it) }
				"capture_rate" -> gameDataIntFilterValue("capture_rate", rawValue)?.let { where(table.captureRate eq it) }
				"base_happiness" -> gameDataIntFilterValue("base_happiness", rawValue)?.let { where(table.baseHappiness eq it) }
				"hatch_counter" -> gameDataIntFilterValue("hatch_counter", rawValue)?.let { where(table.hatchCounter eq it) }
				"baby" -> gameDataBooleanFilterValue("baby", rawValue)?.let { where(table.baby eq it) }
				"legendary" -> gameDataBooleanFilterValue("legendary", rawValue)?.let { where(table.legendary eq it) }
				"mythical" -> gameDataBooleanFilterValue("mythical", rawValue)?.let { where(table.mythical eq it) }
				"enabled" -> gameDataBooleanFilterValue("enabled", rawValue)?.let { where(table.enabled eq it) }
					else -> invalidValue(field, "筛选字段不存在: $field")
				}
			}
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): GameSpeciesResponse =
		entityByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: GameSpeciesRequest): GameSpeciesResponse =
		repository.save(
			GameSpecies {
				code = request.code.orEmpty().requiredSlugCode("code")
				name = gameDataRequiredText(request.name, "name", 120)
				colorId = request.colorId
				shapeId = request.shapeId
				habitatId = request.habitatId
				genderRate = request.genderRate
				captureRate = request.captureRate
				baseHappiness = request.baseHappiness
				hatchCounter = request.hatchCounter
				baby = request.baby ?: false
				legendary = request.legendary ?: false
				mythical = request.mythical ?: false
				enabled = request.enabled ?: true
			},
			SaveMode.INSERT_ONLY,
		).toResponse()

	@Transactional
	fun update(id: Long, request: GameSpeciesRequest): GameSpeciesResponse {
		entityByIdOrNotFound(id)
		return repository.save(
			GameSpecies {
				this.id = id
				code = request.code.orEmpty().requiredSlugCode("code")
				name = gameDataRequiredText(request.name, "name", 120)
				colorId = request.colorId
				shapeId = request.shapeId
				habitatId = request.habitatId
				genderRate = request.genderRate
				captureRate = request.captureRate
				baseHappiness = request.baseHappiness
				hatchCounter = request.hatchCounter
				baby = request.baby ?: false
				legendary = request.legendary ?: false
				mythical = request.mythical ?: false
				enabled = request.enabled ?: true
			},
			SaveMode.UPDATE_ONLY,
		).toResponse()
	}

	@Transactional
	fun delete(id: Long) {
		entityByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun entityByIdOrNotFound(id: Long): GameSpecies =
		repository.findNullable(id) ?: notFound("id", "种类资料不存在: $id")

	private fun GameSpecies.toResponse(): GameSpeciesResponse =
		GameSpeciesResponse {
			id = this@toResponse.id
			code = this@toResponse.code
			name = this@toResponse.name
			colorId = this@toResponse.colorId
			shapeId = this@toResponse.shapeId
			habitatId = this@toResponse.habitatId
			genderRate = this@toResponse.genderRate
			captureRate = this@toResponse.captureRate
			baseHappiness = this@toResponse.baseHappiness
			hatchCounter = this@toResponse.hatchCounter
			baby = this@toResponse.baby
			legendary = this@toResponse.legendary
			mythical = this@toResponse.mythical
			enabled = this@toResponse.enabled
		}
}
