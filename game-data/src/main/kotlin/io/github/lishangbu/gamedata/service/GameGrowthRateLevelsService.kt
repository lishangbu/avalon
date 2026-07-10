package io.github.lishangbu.gamedata.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.dto.GameGrowthRateLevelsRequest
import io.github.lishangbu.gamedata.dto.GameGrowthRateLevelsResponse
import io.github.lishangbu.gamedata.entity.GameGrowthRateLevels
import io.github.lishangbu.gamedata.entity.experience
import io.github.lishangbu.gamedata.entity.growthRateId
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.entity.level
import io.github.lishangbu.gamedata.repository.GameGrowthRateLevelsRepository
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
 * 成长等级经验维护服务。
 *
 * 查询、校验和写入只依赖当前资料实体，不共享跨表运行时元数据。
 */
@Service
class GameGrowthRateLevelsService(
	private val repository: GameGrowthRateLevelsRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): Page<GameGrowthRateLevelsResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(GameGrowthRateLevels::class) {
			search.pattern?.let { pattern ->
				where(or(sql<String>("cast(%e as text)", table.growthRateId) ilike pattern, sql<String>("cast(%e as text)", table.level) ilike pattern))
			}
			filters.forEach { (field, rawValue) ->
				when (field) {
				"growth_rate_id" -> gameDataLongFilterValue("growth_rate_id", rawValue)?.let { where(table.growthRateId eq it) }
				"level" -> gameDataIntFilterValue("level", rawValue)?.let { where(table.level eq it) }
				"experience" -> gameDataIntFilterValue("experience", rawValue)?.let { where(table.experience eq it) }
					else -> invalidValue(field, "筛选字段不存在: $field")
				}
			}
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): GameGrowthRateLevelsResponse =
		entityByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: GameGrowthRateLevelsRequest): GameGrowthRateLevelsResponse =
		repository.save(
			GameGrowthRateLevels {
				growthRateId = request.growthRateId ?: invalidValue("growth_rate_id", "growth_rate_id 不能为空")
				level = request.level ?: invalidValue("level", "level 不能为空")
				experience = request.experience ?: invalidValue("experience", "experience 不能为空")
			},
			SaveMode.INSERT_ONLY,
		).toResponse()

	@Transactional
	fun update(id: Long, request: GameGrowthRateLevelsRequest): GameGrowthRateLevelsResponse {
		entityByIdOrNotFound(id)
		return repository.save(
			GameGrowthRateLevels {
				this.id = id
				growthRateId = request.growthRateId ?: invalidValue("growth_rate_id", "growth_rate_id 不能为空")
				level = request.level ?: invalidValue("level", "level 不能为空")
				experience = request.experience ?: invalidValue("experience", "experience 不能为空")
			},
			SaveMode.UPDATE_ONLY,
		).toResponse()
	}

	@Transactional
	fun delete(id: Long) {
		entityByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun entityByIdOrNotFound(id: Long): GameGrowthRateLevels =
		repository.findNullable(id) ?: notFound("id", "成长等级经验不存在: $id")

	private fun GameGrowthRateLevels.toResponse(): GameGrowthRateLevelsResponse =
		GameGrowthRateLevelsResponse {
			id = this@toResponse.id
			growthRateId = this@toResponse.growthRateId
			level = this@toResponse.level
			experience = this@toResponse.experience
		}
}
