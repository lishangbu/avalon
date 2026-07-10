package io.github.lishangbu.gamedata.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.dto.GameLocationAreaEncountersRequest
import io.github.lishangbu.gamedata.dto.GameLocationAreaEncountersResponse
import io.github.lishangbu.gamedata.entity.GameLocationAreaEncounters
import io.github.lishangbu.gamedata.entity.areaId
import io.github.lishangbu.gamedata.entity.chance
import io.github.lishangbu.gamedata.entity.creatureId
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.entity.maxChance
import io.github.lishangbu.gamedata.entity.maxLevel
import io.github.lishangbu.gamedata.entity.methodId
import io.github.lishangbu.gamedata.entity.minLevel
import io.github.lishangbu.gamedata.repository.GameLocationAreaEncountersRepository
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
 * 区域精灵遭遇维护服务。
 *
 * 查询、校验和写入只依赖当前资料实体，不共享跨表运行时元数据。
 */
@Service
class GameLocationAreaEncountersService(
	private val repository: GameLocationAreaEncountersRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): Page<GameLocationAreaEncountersResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(GameLocationAreaEncounters::class) {
			search.pattern?.let { pattern ->
				where(or(sql<String>("cast(%e as text)", table.areaId) ilike pattern, sql<String>("cast(%e as text)", table.creatureId) ilike pattern))
			}
			filters.forEach { (field, rawValue) ->
				when (field) {
				"area_id" -> gameDataLongFilterValue("area_id", rawValue)?.let { where(table.areaId eq it) }
				"creature_id" -> gameDataLongFilterValue("creature_id", rawValue)?.let { where(table.creatureId eq it) }
				"method_id" -> gameDataLongFilterValue("method_id", rawValue)?.let { where(table.methodId eq it) }
				"min_level" -> gameDataIntFilterValue("min_level", rawValue)?.let { where(table.minLevel eq it) }
				"max_level" -> gameDataIntFilterValue("max_level", rawValue)?.let { where(table.maxLevel eq it) }
				"chance" -> gameDataIntFilterValue("chance", rawValue)?.let { where(table.chance eq it) }
				"max_chance" -> gameDataIntFilterValue("max_chance", rawValue)?.let { where(table.maxChance eq it) }
					else -> invalidValue(field, "筛选字段不存在: $field")
				}
			}
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): GameLocationAreaEncountersResponse =
		entityByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: GameLocationAreaEncountersRequest): GameLocationAreaEncountersResponse =
		repository.save(
			GameLocationAreaEncounters {
				areaId = request.areaId ?: invalidValue("area_id", "area_id 不能为空")
				creatureId = request.creatureId ?: invalidValue("creature_id", "creature_id 不能为空")
				methodId = request.methodId ?: invalidValue("method_id", "method_id 不能为空")
				minLevel = request.minLevel ?: invalidValue("min_level", "min_level 不能为空")
				maxLevel = request.maxLevel ?: invalidValue("max_level", "max_level 不能为空")
				chance = request.chance ?: invalidValue("chance", "chance 不能为空")
				maxChance = request.maxChance ?: invalidValue("max_chance", "max_chance 不能为空")
			},
			SaveMode.INSERT_ONLY,
		).toResponse()

	@Transactional
	fun update(id: Long, request: GameLocationAreaEncountersRequest): GameLocationAreaEncountersResponse {
		entityByIdOrNotFound(id)
		return repository.save(
			GameLocationAreaEncounters {
				this.id = id
				areaId = request.areaId ?: invalidValue("area_id", "area_id 不能为空")
				creatureId = request.creatureId ?: invalidValue("creature_id", "creature_id 不能为空")
				methodId = request.methodId ?: invalidValue("method_id", "method_id 不能为空")
				minLevel = request.minLevel ?: invalidValue("min_level", "min_level 不能为空")
				maxLevel = request.maxLevel ?: invalidValue("max_level", "max_level 不能为空")
				chance = request.chance ?: invalidValue("chance", "chance 不能为空")
				maxChance = request.maxChance ?: invalidValue("max_chance", "max_chance 不能为空")
			},
			SaveMode.UPDATE_ONLY,
		).toResponse()
	}

	@Transactional
	fun delete(id: Long) {
		entityByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun entityByIdOrNotFound(id: Long): GameLocationAreaEncounters =
		repository.findNullable(id) ?: notFound("id", "区域精灵遭遇不存在: $id")

	private fun GameLocationAreaEncounters.toResponse(): GameLocationAreaEncountersResponse =
		GameLocationAreaEncountersResponse {
			id = this@toResponse.id
			areaId = this@toResponse.areaId
			creatureId = this@toResponse.creatureId
			methodId = this@toResponse.methodId
			minLevel = this@toResponse.minLevel
			maxLevel = this@toResponse.maxLevel
			chance = this@toResponse.chance
			maxChance = this@toResponse.maxChance
		}
}
