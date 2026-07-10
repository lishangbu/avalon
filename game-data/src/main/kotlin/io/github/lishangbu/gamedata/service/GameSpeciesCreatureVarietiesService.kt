package io.github.lishangbu.gamedata.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.dto.GameSpeciesCreatureVarietiesRequest
import io.github.lishangbu.gamedata.dto.GameSpeciesCreatureVarietiesResponse
import io.github.lishangbu.gamedata.entity.GameSpeciesCreatureVarieties
import io.github.lishangbu.gamedata.entity.creatureId
import io.github.lishangbu.gamedata.entity.defaultVariety
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.entity.speciesId
import io.github.lishangbu.gamedata.repository.GameSpeciesCreatureVarietiesRepository
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
 * 种类精灵变种维护服务。
 *
 * 查询、校验和写入只依赖当前资料实体，不共享跨表运行时元数据。
 */
@Service
class GameSpeciesCreatureVarietiesService(
	private val repository: GameSpeciesCreatureVarietiesRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): Page<GameSpeciesCreatureVarietiesResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(GameSpeciesCreatureVarieties::class) {
			search.pattern?.let { pattern ->
				where(or(sql<String>("cast(%e as text)", table.speciesId) ilike pattern, sql<String>("cast(%e as text)", table.creatureId) ilike pattern))
			}
			filters.forEach { (field, rawValue) ->
				when (field) {
				"species_id" -> gameDataLongFilterValue("species_id", rawValue)?.let { where(table.speciesId eq it) }
				"creature_id" -> gameDataLongFilterValue("creature_id", rawValue)?.let { where(table.creatureId eq it) }
				"default_variety" -> gameDataBooleanFilterValue("default_variety", rawValue)?.let { where(table.defaultVariety eq it) }
					else -> invalidValue(field, "筛选字段不存在: $field")
				}
			}
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): GameSpeciesCreatureVarietiesResponse =
		entityByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: GameSpeciesCreatureVarietiesRequest): GameSpeciesCreatureVarietiesResponse =
		repository.save(
			GameSpeciesCreatureVarieties {
				speciesId = request.speciesId ?: invalidValue("species_id", "species_id 不能为空")
				creatureId = request.creatureId ?: invalidValue("creature_id", "creature_id 不能为空")
				defaultVariety = request.defaultVariety ?: invalidValue("default_variety", "default_variety 不能为空")
			},
			SaveMode.INSERT_ONLY,
		).toResponse()

	@Transactional
	fun update(id: Long, request: GameSpeciesCreatureVarietiesRequest): GameSpeciesCreatureVarietiesResponse {
		entityByIdOrNotFound(id)
		return repository.save(
			GameSpeciesCreatureVarieties {
				this.id = id
				speciesId = request.speciesId ?: invalidValue("species_id", "species_id 不能为空")
				creatureId = request.creatureId ?: invalidValue("creature_id", "creature_id 不能为空")
				defaultVariety = request.defaultVariety ?: invalidValue("default_variety", "default_variety 不能为空")
			},
			SaveMode.UPDATE_ONLY,
		).toResponse()
	}

	@Transactional
	fun delete(id: Long) {
		entityByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun entityByIdOrNotFound(id: Long): GameSpeciesCreatureVarieties =
		repository.findNullable(id) ?: notFound("id", "种类精灵变种不存在: $id")

	private fun GameSpeciesCreatureVarieties.toResponse(): GameSpeciesCreatureVarietiesResponse =
		GameSpeciesCreatureVarietiesResponse {
			id = this@toResponse.id
			speciesId = this@toResponse.speciesId
			creatureId = this@toResponse.creatureId
			defaultVariety = this@toResponse.defaultVariety
		}
}
