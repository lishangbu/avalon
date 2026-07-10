package io.github.lishangbu.gamedata.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.dto.GameEvolutionDetailsRequest
import io.github.lishangbu.gamedata.dto.GameEvolutionDetailsResponse
import io.github.lishangbu.gamedata.entity.GameEvolutionDetails
import io.github.lishangbu.gamedata.entity.chainId
import io.github.lishangbu.gamedata.entity.fromSpeciesId
import io.github.lishangbu.gamedata.entity.genderId
import io.github.lishangbu.gamedata.entity.heldItemId
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.entity.isDefault
import io.github.lishangbu.gamedata.entity.itemId
import io.github.lishangbu.gamedata.entity.knownElementId
import io.github.lishangbu.gamedata.entity.knownSkillId
import io.github.lishangbu.gamedata.entity.locationId
import io.github.lishangbu.gamedata.entity.minAffection
import io.github.lishangbu.gamedata.entity.minBeauty
import io.github.lishangbu.gamedata.entity.minDamageTaken
import io.github.lishangbu.gamedata.entity.minHappiness
import io.github.lishangbu.gamedata.entity.minLevel
import io.github.lishangbu.gamedata.entity.minMoveCount
import io.github.lishangbu.gamedata.entity.minSteps
import io.github.lishangbu.gamedata.entity.nearSpecialRock
import io.github.lishangbu.gamedata.entity.needsMultiplayer
import io.github.lishangbu.gamedata.entity.needsOverworldRain
import io.github.lishangbu.gamedata.entity.partyElementId
import io.github.lishangbu.gamedata.entity.partySpeciesId
import io.github.lishangbu.gamedata.entity.regionId
import io.github.lishangbu.gamedata.entity.relativePhysicalStats
import io.github.lishangbu.gamedata.entity.timeOfDay
import io.github.lishangbu.gamedata.entity.toSpeciesId
import io.github.lishangbu.gamedata.entity.tradeSpeciesId
import io.github.lishangbu.gamedata.entity.triggerId
import io.github.lishangbu.gamedata.entity.turnUpsideDown
import io.github.lishangbu.gamedata.repository.GameEvolutionDetailsRepository
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
 * 进化条件维护服务。
 *
 * 查询、校验和写入只依赖当前资料实体，不共享跨表运行时元数据。
 */
@Service
class GameEvolutionDetailsService(
	private val repository: GameEvolutionDetailsRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): Page<GameEvolutionDetailsResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(GameEvolutionDetails::class) {
			search.pattern?.let { pattern ->
				where(or(sql<String>("cast(%e as text)", table.chainId) ilike pattern, sql<String>("cast(%e as text)", table.fromSpeciesId) ilike pattern, sql<String>("cast(%e as text)", table.toSpeciesId) ilike pattern))
			}
			filters.forEach { (field, rawValue) ->
				when (field) {
				"chain_id" -> gameDataLongFilterValue("chain_id", rawValue)?.let { where(table.chainId eq it) }
				"from_species_id" -> gameDataLongFilterValue("from_species_id", rawValue)?.let { where(table.fromSpeciesId eq it) }
				"to_species_id" -> gameDataLongFilterValue("to_species_id", rawValue)?.let { where(table.toSpeciesId eq it) }
				"trigger_id" -> gameDataLongFilterValue("trigger_id", rawValue)?.let { where(table.triggerId eq it) }
				"item_id" -> gameDataLongFilterValue("item_id", rawValue)?.let { where(table.itemId eq it) }
				"held_item_id" -> gameDataLongFilterValue("held_item_id", rawValue)?.let { where(table.heldItemId eq it) }
				"known_skill_id" -> gameDataLongFilterValue("known_skill_id", rawValue)?.let { where(table.knownSkillId eq it) }
				"known_element_id" -> gameDataLongFilterValue("known_element_id", rawValue)?.let { where(table.knownElementId eq it) }
				"location_id" -> gameDataLongFilterValue("location_id", rawValue)?.let { where(table.locationId eq it) }
				"party_species_id" -> gameDataLongFilterValue("party_species_id", rawValue)?.let { where(table.partySpeciesId eq it) }
				"party_element_id" -> gameDataLongFilterValue("party_element_id", rawValue)?.let { where(table.partyElementId eq it) }
				"trade_species_id" -> gameDataLongFilterValue("trade_species_id", rawValue)?.let { where(table.tradeSpeciesId eq it) }
				"gender_id" -> gameDataLongFilterValue("gender_id", rawValue)?.let { where(table.genderId eq it) }
				"region_id" -> gameDataLongFilterValue("region_id", rawValue)?.let { where(table.regionId eq it) }
				"min_level" -> gameDataIntFilterValue("min_level", rawValue)?.let { where(table.minLevel eq it) }
				"min_happiness" -> gameDataIntFilterValue("min_happiness", rawValue)?.let { where(table.minHappiness eq it) }
				"min_beauty" -> gameDataIntFilterValue("min_beauty", rawValue)?.let { where(table.minBeauty eq it) }
				"min_affection" -> gameDataIntFilterValue("min_affection", rawValue)?.let { where(table.minAffection eq it) }
				"relative_physical_stats" -> gameDataIntFilterValue("relative_physical_stats", rawValue)?.let { where(table.relativePhysicalStats eq it) }
				"min_damage_taken" -> gameDataIntFilterValue("min_damage_taken", rawValue)?.let { where(table.minDamageTaken eq it) }
				"min_move_count" -> gameDataIntFilterValue("min_move_count", rawValue)?.let { where(table.minMoveCount eq it) }
				"min_steps" -> gameDataIntFilterValue("min_steps", rawValue)?.let { where(table.minSteps eq it) }
				"time_of_day" -> gameDataStringFilterValue("time_of_day", rawValue)?.let { where(table.timeOfDay eq it) }
				"needs_overworld_rain" -> gameDataBooleanFilterValue("needs_overworld_rain", rawValue)?.let { where(table.needsOverworldRain eq it) }
				"turn_upside_down" -> gameDataBooleanFilterValue("turn_upside_down", rawValue)?.let { where(table.turnUpsideDown eq it) }
				"near_special_rock" -> gameDataBooleanFilterValue("near_special_rock", rawValue)?.let { where(table.nearSpecialRock eq it) }
				"needs_multiplayer" -> gameDataBooleanFilterValue("needs_multiplayer", rawValue)?.let { where(table.needsMultiplayer eq it) }
				"is_default" -> gameDataBooleanFilterValue("is_default", rawValue)?.let { where(table.isDefault eq it) }
					else -> invalidValue(field, "筛选字段不存在: $field")
				}
			}
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): GameEvolutionDetailsResponse =
		entityByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: GameEvolutionDetailsRequest): GameEvolutionDetailsResponse =
		repository.save(
			GameEvolutionDetails {
				chainId = request.chainId ?: invalidValue("chain_id", "chain_id 不能为空")
				fromSpeciesId = request.fromSpeciesId ?: invalidValue("from_species_id", "from_species_id 不能为空")
				toSpeciesId = request.toSpeciesId ?: invalidValue("to_species_id", "to_species_id 不能为空")
				triggerId = request.triggerId
				itemId = request.itemId
				heldItemId = request.heldItemId
				knownSkillId = request.knownSkillId
				knownElementId = request.knownElementId
				locationId = request.locationId
				partySpeciesId = request.partySpeciesId
				partyElementId = request.partyElementId
				tradeSpeciesId = request.tradeSpeciesId
				genderId = request.genderId
				regionId = request.regionId
				minLevel = request.minLevel
				minHappiness = request.minHappiness
				minBeauty = request.minBeauty
				minAffection = request.minAffection
				relativePhysicalStats = request.relativePhysicalStats
				minDamageTaken = request.minDamageTaken
				minMoveCount = request.minMoveCount
				minSteps = request.minSteps
				timeOfDay = gameDataOptionalText(request.timeOfDay, "time_of_day", 40)
				needsOverworldRain = request.needsOverworldRain
				turnUpsideDown = request.turnUpsideDown
				nearSpecialRock = request.nearSpecialRock
				needsMultiplayer = request.needsMultiplayer
				isDefault = request.isDefault
			},
			SaveMode.INSERT_ONLY,
		).toResponse()

	@Transactional
	fun update(id: Long, request: GameEvolutionDetailsRequest): GameEvolutionDetailsResponse {
		entityByIdOrNotFound(id)
		return repository.save(
			GameEvolutionDetails {
				this.id = id
				chainId = request.chainId ?: invalidValue("chain_id", "chain_id 不能为空")
				fromSpeciesId = request.fromSpeciesId ?: invalidValue("from_species_id", "from_species_id 不能为空")
				toSpeciesId = request.toSpeciesId ?: invalidValue("to_species_id", "to_species_id 不能为空")
				triggerId = request.triggerId
				itemId = request.itemId
				heldItemId = request.heldItemId
				knownSkillId = request.knownSkillId
				knownElementId = request.knownElementId
				locationId = request.locationId
				partySpeciesId = request.partySpeciesId
				partyElementId = request.partyElementId
				tradeSpeciesId = request.tradeSpeciesId
				genderId = request.genderId
				regionId = request.regionId
				minLevel = request.minLevel
				minHappiness = request.minHappiness
				minBeauty = request.minBeauty
				minAffection = request.minAffection
				relativePhysicalStats = request.relativePhysicalStats
				minDamageTaken = request.minDamageTaken
				minMoveCount = request.minMoveCount
				minSteps = request.minSteps
				timeOfDay = gameDataOptionalText(request.timeOfDay, "time_of_day", 40)
				needsOverworldRain = request.needsOverworldRain
				turnUpsideDown = request.turnUpsideDown
				nearSpecialRock = request.nearSpecialRock
				needsMultiplayer = request.needsMultiplayer
				isDefault = request.isDefault
			},
			SaveMode.UPDATE_ONLY,
		).toResponse()
	}

	@Transactional
	fun delete(id: Long) {
		entityByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun entityByIdOrNotFound(id: Long): GameEvolutionDetails =
		repository.findNullable(id) ?: notFound("id", "进化条件不存在: $id")

	private fun GameEvolutionDetails.toResponse(): GameEvolutionDetailsResponse =
		GameEvolutionDetailsResponse {
			id = this@toResponse.id
			chainId = this@toResponse.chainId
			fromSpeciesId = this@toResponse.fromSpeciesId
			toSpeciesId = this@toResponse.toSpeciesId
			triggerId = this@toResponse.triggerId
			itemId = this@toResponse.itemId
			heldItemId = this@toResponse.heldItemId
			knownSkillId = this@toResponse.knownSkillId
			knownElementId = this@toResponse.knownElementId
			locationId = this@toResponse.locationId
			partySpeciesId = this@toResponse.partySpeciesId
			partyElementId = this@toResponse.partyElementId
			tradeSpeciesId = this@toResponse.tradeSpeciesId
			genderId = this@toResponse.genderId
			regionId = this@toResponse.regionId
			minLevel = this@toResponse.minLevel
			minHappiness = this@toResponse.minHappiness
			minBeauty = this@toResponse.minBeauty
			minAffection = this@toResponse.minAffection
			relativePhysicalStats = this@toResponse.relativePhysicalStats
			minDamageTaken = this@toResponse.minDamageTaken
			minMoveCount = this@toResponse.minMoveCount
			minSteps = this@toResponse.minSteps
			timeOfDay = this@toResponse.timeOfDay
			needsOverworldRain = this@toResponse.needsOverworldRain
			turnUpsideDown = this@toResponse.turnUpsideDown
			nearSpecialRock = this@toResponse.nearSpecialRock
			needsMultiplayer = this@toResponse.needsMultiplayer
			isDefault = this@toResponse.isDefault
		}
}
