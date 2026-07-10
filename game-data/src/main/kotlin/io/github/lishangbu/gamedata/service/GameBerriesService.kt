package io.github.lishangbu.gamedata.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.dto.GameBerriesRequest
import io.github.lishangbu.gamedata.dto.GameBerriesResponse
import io.github.lishangbu.gamedata.entity.GameBerries
import io.github.lishangbu.gamedata.entity.code
import io.github.lishangbu.gamedata.entity.enabled
import io.github.lishangbu.gamedata.entity.firmnessId
import io.github.lishangbu.gamedata.entity.growthTime
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.entity.itemId
import io.github.lishangbu.gamedata.entity.maxHarvest
import io.github.lishangbu.gamedata.entity.name
import io.github.lishangbu.gamedata.entity.naturalGiftElementId
import io.github.lishangbu.gamedata.entity.naturalGiftPower
import io.github.lishangbu.gamedata.entity.size
import io.github.lishangbu.gamedata.entity.smoothness
import io.github.lishangbu.gamedata.entity.soilDryness
import io.github.lishangbu.gamedata.repository.GameBerriesRepository
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
 * 树果资料维护服务。
 *
 * 查询、校验和写入只依赖当前资料实体，不共享跨表运行时元数据。
 */
@Service
class GameBerriesService(
	private val repository: GameBerriesRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): Page<GameBerriesResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(GameBerries::class) {
			search.pattern?.let { pattern ->
				where(or(table.code ilike pattern, table.name ilike pattern))
			}
			filters.forEach { (field, rawValue) ->
				when (field) {
				"code" -> gameDataStringFilterValue("code", rawValue)?.let { where(table.code eq it) }
				"name" -> gameDataStringFilterValue("name", rawValue)?.let { where(table.name eq it) }
				"item_id" -> gameDataLongFilterValue("item_id", rawValue)?.let { where(table.itemId eq it) }
				"firmness_id" -> gameDataLongFilterValue("firmness_id", rawValue)?.let { where(table.firmnessId eq it) }
				"natural_gift_element_id" -> gameDataLongFilterValue("natural_gift_element_id", rawValue)?.let { where(table.naturalGiftElementId eq it) }
				"growth_time" -> gameDataIntFilterValue("growth_time", rawValue)?.let { where(table.growthTime eq it) }
				"max_harvest" -> gameDataIntFilterValue("max_harvest", rawValue)?.let { where(table.maxHarvest eq it) }
				"natural_gift_power" -> gameDataIntFilterValue("natural_gift_power", rawValue)?.let { where(table.naturalGiftPower eq it) }
				"size" -> gameDataIntFilterValue("size", rawValue)?.let { where(table.size eq it) }
				"smoothness" -> gameDataIntFilterValue("smoothness", rawValue)?.let { where(table.smoothness eq it) }
				"soil_dryness" -> gameDataIntFilterValue("soil_dryness", rawValue)?.let { where(table.soilDryness eq it) }
				"enabled" -> gameDataBooleanFilterValue("enabled", rawValue)?.let { where(table.enabled eq it) }
					else -> invalidValue(field, "筛选字段不存在: $field")
				}
			}
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): GameBerriesResponse =
		entityByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: GameBerriesRequest): GameBerriesResponse =
		repository.save(
			GameBerries {
				code = request.code.orEmpty().requiredSlugCode("code")
				name = gameDataRequiredText(request.name, "name", 120)
				itemId = request.itemId
				firmnessId = request.firmnessId
				naturalGiftElementId = request.naturalGiftElementId
				growthTime = request.growthTime
				maxHarvest = request.maxHarvest
				naturalGiftPower = request.naturalGiftPower
				size = request.size
				smoothness = request.smoothness
				soilDryness = request.soilDryness
				enabled = request.enabled ?: invalidValue("enabled", "enabled 不能为空")
			},
			SaveMode.INSERT_ONLY,
		).toResponse()

	@Transactional
	fun update(id: Long, request: GameBerriesRequest): GameBerriesResponse {
		entityByIdOrNotFound(id)
		return repository.save(
			GameBerries {
				this.id = id
				code = request.code.orEmpty().requiredSlugCode("code")
				name = gameDataRequiredText(request.name, "name", 120)
				itemId = request.itemId
				firmnessId = request.firmnessId
				naturalGiftElementId = request.naturalGiftElementId
				growthTime = request.growthTime
				maxHarvest = request.maxHarvest
				naturalGiftPower = request.naturalGiftPower
				size = request.size
				smoothness = request.smoothness
				soilDryness = request.soilDryness
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

	private fun entityByIdOrNotFound(id: Long): GameBerries =
		repository.findNullable(id) ?: notFound("id", "树果资料不存在: $id")

	private fun GameBerries.toResponse(): GameBerriesResponse =
		GameBerriesResponse {
			id = this@toResponse.id
			code = this@toResponse.code
			name = this@toResponse.name
			itemId = this@toResponse.itemId
			firmnessId = this@toResponse.firmnessId
			naturalGiftElementId = this@toResponse.naturalGiftElementId
			growthTime = this@toResponse.growthTime
			maxHarvest = this@toResponse.maxHarvest
			naturalGiftPower = this@toResponse.naturalGiftPower
			size = this@toResponse.size
			smoothness = this@toResponse.smoothness
			soilDryness = this@toResponse.soilDryness
			enabled = this@toResponse.enabled
		}
}
