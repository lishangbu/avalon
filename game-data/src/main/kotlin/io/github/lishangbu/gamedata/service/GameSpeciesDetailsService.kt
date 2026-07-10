package io.github.lishangbu.gamedata.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.dto.GameSpeciesDetailsRequest
import io.github.lishangbu.gamedata.dto.GameSpeciesDetailsResponse
import io.github.lishangbu.gamedata.entity.GameSpeciesDetails
import io.github.lishangbu.gamedata.entity.evolutionChainId
import io.github.lishangbu.gamedata.entity.evolvesFromSpeciesId
import io.github.lishangbu.gamedata.entity.flavorText
import io.github.lishangbu.gamedata.entity.formsSwitchable
import io.github.lishangbu.gamedata.entity.genderDifferences
import io.github.lishangbu.gamedata.entity.genus
import io.github.lishangbu.gamedata.entity.growthRateId
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.entity.sortOrder
import io.github.lishangbu.gamedata.entity.speciesId
import io.github.lishangbu.gamedata.repository.GameSpeciesDetailsRepository
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
 * 种类详情维护服务。
 *
 * 查询、校验和写入只依赖当前资料实体，不共享跨表运行时元数据。
 */
@Service
class GameSpeciesDetailsService(
	private val repository: GameSpeciesDetailsRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): Page<GameSpeciesDetailsResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(GameSpeciesDetails::class) {
			search.pattern?.let { pattern ->
				where(or(sql<String>("cast(%e as text)", table.speciesId) ilike pattern, table.genus ilike pattern, table.flavorText ilike pattern))
			}
			filters.forEach { (field, rawValue) ->
				when (field) {
				"species_id" -> gameDataLongFilterValue("species_id", rawValue)?.let { where(table.speciesId eq it) }
				"growth_rate_id" -> gameDataLongFilterValue("growth_rate_id", rawValue)?.let { where(table.growthRateId eq it) }
				"evolves_from_species_id" -> gameDataLongFilterValue("evolves_from_species_id", rawValue)?.let { where(table.evolvesFromSpeciesId eq it) }
				"evolution_chain_id" -> gameDataLongFilterValue("evolution_chain_id", rawValue)?.let { where(table.evolutionChainId eq it) }
				"sort_order" -> gameDataIntFilterValue("sort_order", rawValue)?.let { where(table.sortOrder eq it) }
				"gender_differences" -> gameDataBooleanFilterValue("gender_differences", rawValue)?.let { where(table.genderDifferences eq it) }
				"forms_switchable" -> gameDataBooleanFilterValue("forms_switchable", rawValue)?.let { where(table.formsSwitchable eq it) }
				"genus" -> gameDataStringFilterValue("genus", rawValue)?.let { where(table.genus eq it) }
				"flavor_text" -> gameDataStringFilterValue("flavor_text", rawValue)?.let { where(table.flavorText eq it) }
					else -> invalidValue(field, "筛选字段不存在: $field")
				}
			}
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): GameSpeciesDetailsResponse =
		entityByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: GameSpeciesDetailsRequest): GameSpeciesDetailsResponse =
		repository.save(
			GameSpeciesDetails {
				speciesId = request.speciesId ?: invalidValue("species_id", "species_id 不能为空")
				growthRateId = request.growthRateId
				evolvesFromSpeciesId = request.evolvesFromSpeciesId
				evolutionChainId = request.evolutionChainId
				sortOrder = request.sortOrder
				genderDifferences = request.genderDifferences ?: invalidValue("gender_differences", "gender_differences 不能为空")
				formsSwitchable = request.formsSwitchable ?: invalidValue("forms_switchable", "forms_switchable 不能为空")
				genus = gameDataOptionalText(request.genus, "genus", 200)
				flavorText = gameDataOptionalText(request.flavorText, "flavor_text", null)
			},
			SaveMode.INSERT_ONLY,
		).toResponse()

	@Transactional
	fun update(id: Long, request: GameSpeciesDetailsRequest): GameSpeciesDetailsResponse {
		entityByIdOrNotFound(id)
		return repository.save(
			GameSpeciesDetails {
				this.id = id
				speciesId = request.speciesId ?: invalidValue("species_id", "species_id 不能为空")
				growthRateId = request.growthRateId
				evolvesFromSpeciesId = request.evolvesFromSpeciesId
				evolutionChainId = request.evolutionChainId
				sortOrder = request.sortOrder
				genderDifferences = request.genderDifferences ?: invalidValue("gender_differences", "gender_differences 不能为空")
				formsSwitchable = request.formsSwitchable ?: invalidValue("forms_switchable", "forms_switchable 不能为空")
				genus = gameDataOptionalText(request.genus, "genus", 200)
				flavorText = gameDataOptionalText(request.flavorText, "flavor_text", null)
			},
			SaveMode.UPDATE_ONLY,
		).toResponse()
	}

	@Transactional
	fun delete(id: Long) {
		entityByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun entityByIdOrNotFound(id: Long): GameSpeciesDetails =
		repository.findNullable(id) ?: notFound("id", "种类详情不存在: $id")

	private fun GameSpeciesDetails.toResponse(): GameSpeciesDetailsResponse =
		GameSpeciesDetailsResponse {
			id = this@toResponse.id
			speciesId = this@toResponse.speciesId
			growthRateId = this@toResponse.growthRateId
			evolvesFromSpeciesId = this@toResponse.evolvesFromSpeciesId
			evolutionChainId = this@toResponse.evolutionChainId
			sortOrder = this@toResponse.sortOrder
			genderDifferences = this@toResponse.genderDifferences
			formsSwitchable = this@toResponse.formsSwitchable
			genus = this@toResponse.genus
			flavorText = this@toResponse.flavorText
		}
}
