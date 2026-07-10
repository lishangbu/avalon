package io.github.lishangbu.gamedata.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.dto.GameNatureBattleStylePreferencesRequest
import io.github.lishangbu.gamedata.dto.GameNatureBattleStylePreferencesResponse
import io.github.lishangbu.gamedata.entity.GameNatureBattleStylePreferences
import io.github.lishangbu.gamedata.entity.battleStyleId
import io.github.lishangbu.gamedata.entity.highHpPreference
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.entity.lowHpPreference
import io.github.lishangbu.gamedata.entity.natureId
import io.github.lishangbu.gamedata.repository.GameNatureBattleStylePreferencesRepository
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
 * 性格战斗风格偏好维护服务。
 *
 * 查询、校验和写入只依赖当前资料实体，不共享跨表运行时元数据。
 */
@Service
class GameNatureBattleStylePreferencesService(
	private val repository: GameNatureBattleStylePreferencesRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): Page<GameNatureBattleStylePreferencesResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(GameNatureBattleStylePreferences::class) {
			search.pattern?.let { pattern ->
				where(or(sql<String>("cast(%e as text)", table.natureId) ilike pattern, sql<String>("cast(%e as text)", table.battleStyleId) ilike pattern))
			}
			filters.forEach { (field, rawValue) ->
				when (field) {
				"nature_id" -> gameDataLongFilterValue("nature_id", rawValue)?.let { where(table.natureId eq it) }
				"battle_style_id" -> gameDataLongFilterValue("battle_style_id", rawValue)?.let { where(table.battleStyleId eq it) }
				"low_hp_preference" -> gameDataIntFilterValue("low_hp_preference", rawValue)?.let { where(table.lowHpPreference eq it) }
				"high_hp_preference" -> gameDataIntFilterValue("high_hp_preference", rawValue)?.let { where(table.highHpPreference eq it) }
					else -> invalidValue(field, "筛选字段不存在: $field")
				}
			}
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): GameNatureBattleStylePreferencesResponse =
		entityByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: GameNatureBattleStylePreferencesRequest): GameNatureBattleStylePreferencesResponse =
		repository.save(
			GameNatureBattleStylePreferences {
				natureId = request.natureId ?: invalidValue("nature_id", "nature_id 不能为空")
				battleStyleId = request.battleStyleId ?: invalidValue("battle_style_id", "battle_style_id 不能为空")
				lowHpPreference = request.lowHpPreference ?: invalidValue("low_hp_preference", "low_hp_preference 不能为空")
				highHpPreference = request.highHpPreference ?: invalidValue("high_hp_preference", "high_hp_preference 不能为空")
			},
			SaveMode.INSERT_ONLY,
		).toResponse()

	@Transactional
	fun update(id: Long, request: GameNatureBattleStylePreferencesRequest): GameNatureBattleStylePreferencesResponse {
		entityByIdOrNotFound(id)
		return repository.save(
			GameNatureBattleStylePreferences {
				this.id = id
				natureId = request.natureId ?: invalidValue("nature_id", "nature_id 不能为空")
				battleStyleId = request.battleStyleId ?: invalidValue("battle_style_id", "battle_style_id 不能为空")
				lowHpPreference = request.lowHpPreference ?: invalidValue("low_hp_preference", "low_hp_preference 不能为空")
				highHpPreference = request.highHpPreference ?: invalidValue("high_hp_preference", "high_hp_preference 不能为空")
			},
			SaveMode.UPDATE_ONLY,
		).toResponse()
	}

	@Transactional
	fun delete(id: Long) {
		entityByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun entityByIdOrNotFound(id: Long): GameNatureBattleStylePreferences =
		repository.findNullable(id) ?: notFound("id", "性格战斗风格偏好不存在: $id")

	private fun GameNatureBattleStylePreferences.toResponse(): GameNatureBattleStylePreferencesResponse =
		GameNatureBattleStylePreferencesResponse {
			id = this@toResponse.id
			natureId = this@toResponse.natureId
			battleStyleId = this@toResponse.battleStyleId
			lowHpPreference = this@toResponse.lowHpPreference
			highHpPreference = this@toResponse.highHpPreference
		}
}
