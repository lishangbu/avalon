package io.github.lishangbu.gamedata.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.dto.GameSkillDetailsRequest
import io.github.lishangbu.gamedata.dto.GameSkillDetailsResponse
import io.github.lishangbu.gamedata.entity.GameSkillDetails
import io.github.lishangbu.gamedata.entity.advancedContestEffectId
import io.github.lishangbu.gamedata.entity.ailmentChance
import io.github.lishangbu.gamedata.entity.ailmentId
import io.github.lishangbu.gamedata.entity.categoryId
import io.github.lishangbu.gamedata.entity.contestEffectId
import io.github.lishangbu.gamedata.entity.contestTypeId
import io.github.lishangbu.gamedata.entity.critRate
import io.github.lishangbu.gamedata.entity.drain
import io.github.lishangbu.gamedata.entity.effect
import io.github.lishangbu.gamedata.entity.flavorText
import io.github.lishangbu.gamedata.entity.flinchChance
import io.github.lishangbu.gamedata.entity.healing
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.entity.maxHits
import io.github.lishangbu.gamedata.entity.maxTurns
import io.github.lishangbu.gamedata.entity.minHits
import io.github.lishangbu.gamedata.entity.minTurns
import io.github.lishangbu.gamedata.entity.shortEffect
import io.github.lishangbu.gamedata.entity.skillId
import io.github.lishangbu.gamedata.entity.statChance
import io.github.lishangbu.gamedata.entity.targetId
import io.github.lishangbu.gamedata.repository.GameSkillDetailsRepository
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
 * 技能详情维护服务。
 *
 * 查询、校验和写入只依赖当前资料实体，不共享跨表运行时元数据。
 */
@Service
class GameSkillDetailsService(
	private val repository: GameSkillDetailsRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): Page<GameSkillDetailsResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(GameSkillDetails::class) {
			search.pattern?.let { pattern ->
				where(or(sql<String>("cast(%e as text)", table.skillId) ilike pattern, table.effect ilike pattern, table.flavorText ilike pattern))
			}
			filters.forEach { (field, rawValue) ->
				when (field) {
				"skill_id" -> gameDataLongFilterValue("skill_id", rawValue)?.let { where(table.skillId eq it) }
				"ailment_id" -> gameDataLongFilterValue("ailment_id", rawValue)?.let { where(table.ailmentId eq it) }
				"category_id" -> gameDataLongFilterValue("category_id", rawValue)?.let { where(table.categoryId eq it) }
				"target_id" -> gameDataLongFilterValue("target_id", rawValue)?.let { where(table.targetId eq it) }
				"contest_type_id" -> gameDataLongFilterValue("contest_type_id", rawValue)?.let { where(table.contestTypeId eq it) }
				"contest_effect_id" -> gameDataLongFilterValue("contest_effect_id", rawValue)?.let { where(table.contestEffectId eq it) }
				"advanced_contest_effect_id" -> gameDataLongFilterValue("advanced_contest_effect_id", rawValue)?.let { where(table.advancedContestEffectId eq it) }
				"min_hits" -> gameDataIntFilterValue("min_hits", rawValue)?.let { where(table.minHits eq it) }
				"max_hits" -> gameDataIntFilterValue("max_hits", rawValue)?.let { where(table.maxHits eq it) }
				"min_turns" -> gameDataIntFilterValue("min_turns", rawValue)?.let { where(table.minTurns eq it) }
				"max_turns" -> gameDataIntFilterValue("max_turns", rawValue)?.let { where(table.maxTurns eq it) }
				"drain" -> gameDataIntFilterValue("drain", rawValue)?.let { where(table.drain eq it) }
				"healing" -> gameDataIntFilterValue("healing", rawValue)?.let { where(table.healing eq it) }
				"crit_rate" -> gameDataIntFilterValue("crit_rate", rawValue)?.let { where(table.critRate eq it) }
				"ailment_chance" -> gameDataIntFilterValue("ailment_chance", rawValue)?.let { where(table.ailmentChance eq it) }
				"flinch_chance" -> gameDataIntFilterValue("flinch_chance", rawValue)?.let { where(table.flinchChance eq it) }
				"stat_chance" -> gameDataIntFilterValue("stat_chance", rawValue)?.let { where(table.statChance eq it) }
				"effect" -> gameDataStringFilterValue("effect", rawValue)?.let { where(table.effect eq it) }
				"short_effect" -> gameDataStringFilterValue("short_effect", rawValue)?.let { where(table.shortEffect eq it) }
				"flavor_text" -> gameDataStringFilterValue("flavor_text", rawValue)?.let { where(table.flavorText eq it) }
					else -> invalidValue(field, "筛选字段不存在: $field")
				}
			}
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): GameSkillDetailsResponse =
		entityByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: GameSkillDetailsRequest): GameSkillDetailsResponse =
		repository.save(
			GameSkillDetails {
				skillId = request.skillId ?: invalidValue("skill_id", "skill_id 不能为空")
				ailmentId = request.ailmentId
				categoryId = request.categoryId
				targetId = request.targetId
				contestTypeId = request.contestTypeId
				contestEffectId = request.contestEffectId
				advancedContestEffectId = request.advancedContestEffectId
				minHits = request.minHits
				maxHits = request.maxHits
				minTurns = request.minTurns
				maxTurns = request.maxTurns
				drain = request.drain
				healing = request.healing
				critRate = request.critRate
				ailmentChance = request.ailmentChance
				flinchChance = request.flinchChance
				statChance = request.statChance
				effect = gameDataOptionalText(request.effect, "effect", null)
				shortEffect = gameDataOptionalText(request.shortEffect, "short_effect", null)
				flavorText = gameDataOptionalText(request.flavorText, "flavor_text", null)
			},
			SaveMode.INSERT_ONLY,
		).toResponse()

	@Transactional
	fun update(id: Long, request: GameSkillDetailsRequest): GameSkillDetailsResponse {
		entityByIdOrNotFound(id)
		return repository.save(
			GameSkillDetails {
				this.id = id
				skillId = request.skillId ?: invalidValue("skill_id", "skill_id 不能为空")
				ailmentId = request.ailmentId
				categoryId = request.categoryId
				targetId = request.targetId
				contestTypeId = request.contestTypeId
				contestEffectId = request.contestEffectId
				advancedContestEffectId = request.advancedContestEffectId
				minHits = request.minHits
				maxHits = request.maxHits
				minTurns = request.minTurns
				maxTurns = request.maxTurns
				drain = request.drain
				healing = request.healing
				critRate = request.critRate
				ailmentChance = request.ailmentChance
				flinchChance = request.flinchChance
				statChance = request.statChance
				effect = gameDataOptionalText(request.effect, "effect", null)
				shortEffect = gameDataOptionalText(request.shortEffect, "short_effect", null)
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

	private fun entityByIdOrNotFound(id: Long): GameSkillDetails =
		repository.findNullable(id) ?: notFound("id", "技能详情不存在: $id")

	private fun GameSkillDetails.toResponse(): GameSkillDetailsResponse =
		GameSkillDetailsResponse {
			id = this@toResponse.id
			skillId = this@toResponse.skillId
			ailmentId = this@toResponse.ailmentId
			categoryId = this@toResponse.categoryId
			targetId = this@toResponse.targetId
			contestTypeId = this@toResponse.contestTypeId
			contestEffectId = this@toResponse.contestEffectId
			advancedContestEffectId = this@toResponse.advancedContestEffectId
			minHits = this@toResponse.minHits
			maxHits = this@toResponse.maxHits
			minTurns = this@toResponse.minTurns
			maxTurns = this@toResponse.maxTurns
			drain = this@toResponse.drain
			healing = this@toResponse.healing
			critRate = this@toResponse.critRate
			ailmentChance = this@toResponse.ailmentChance
			flinchChance = this@toResponse.flinchChance
			statChance = this@toResponse.statChance
			effect = this@toResponse.effect
			shortEffect = this@toResponse.shortEffect
			flavorText = this@toResponse.flavorText
		}
}
