package io.github.lishangbu.gamedata.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.dto.GameStatCharacteristicsRequest
import io.github.lishangbu.gamedata.dto.GameStatCharacteristicsResponse
import io.github.lishangbu.gamedata.entity.GameStatCharacteristics
import io.github.lishangbu.gamedata.entity.characteristicId
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.entity.statId
import io.github.lishangbu.gamedata.repository.GameStatCharacteristicsRepository
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
 * 数值项特征维护服务。
 *
 * 查询、校验和写入只依赖当前资料实体，不共享跨表运行时元数据。
 */
@Service
class GameStatCharacteristicsService(
	private val repository: GameStatCharacteristicsRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): Page<GameStatCharacteristicsResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(GameStatCharacteristics::class) {
			search.pattern?.let { pattern ->
				where(or(sql<String>("cast(%e as text)", table.statId) ilike pattern, sql<String>("cast(%e as text)", table.characteristicId) ilike pattern))
			}
			filters.forEach { (field, rawValue) ->
				when (field) {
				"stat_id" -> gameDataLongFilterValue("stat_id", rawValue)?.let { where(table.statId eq it) }
				"characteristic_id" -> gameDataLongFilterValue("characteristic_id", rawValue)?.let { where(table.characteristicId eq it) }
					else -> invalidValue(field, "筛选字段不存在: $field")
				}
			}
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): GameStatCharacteristicsResponse =
		entityByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: GameStatCharacteristicsRequest): GameStatCharacteristicsResponse =
		repository.save(
			GameStatCharacteristics {
				statId = request.statId ?: invalidValue("stat_id", "stat_id 不能为空")
				characteristicId = request.characteristicId ?: invalidValue("characteristic_id", "characteristic_id 不能为空")
			},
			SaveMode.INSERT_ONLY,
		).toResponse()

	@Transactional
	fun update(id: Long, request: GameStatCharacteristicsRequest): GameStatCharacteristicsResponse {
		entityByIdOrNotFound(id)
		return repository.save(
			GameStatCharacteristics {
				this.id = id
				statId = request.statId ?: invalidValue("stat_id", "stat_id 不能为空")
				characteristicId = request.characteristicId ?: invalidValue("characteristic_id", "characteristic_id 不能为空")
			},
			SaveMode.UPDATE_ONLY,
		).toResponse()
	}

	@Transactional
	fun delete(id: Long) {
		entityByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun entityByIdOrNotFound(id: Long): GameStatCharacteristics =
		repository.findNullable(id) ?: notFound("id", "数值项特征不存在: $id")

	private fun GameStatCharacteristics.toResponse(): GameStatCharacteristicsResponse =
		GameStatCharacteristicsResponse {
			id = this@toResponse.id
			statId = this@toResponse.statId
			characteristicId = this@toResponse.characteristicId
		}
}
