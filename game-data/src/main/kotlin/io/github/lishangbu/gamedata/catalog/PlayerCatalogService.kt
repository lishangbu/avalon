package io.github.lishangbu.gamedata.catalog

import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.entity.GameCreature
import io.github.lishangbu.gamedata.entity.GameCreatureSkin
import io.github.lishangbu.gamedata.entity.GameAbility
import io.github.lishangbu.gamedata.entity.GameAbilityDetails
import io.github.lishangbu.gamedata.entity.GameItem
import io.github.lishangbu.gamedata.entity.GameItemDetails
import io.github.lishangbu.gamedata.entity.GameSkill
import io.github.lishangbu.gamedata.entity.GameSkillDetails
import io.github.lishangbu.gamedata.entity.GameSpeciesDetails
import io.github.lishangbu.gamedata.entity.avatarAssetKey
import io.github.lishangbu.gamedata.entity.abilityId
import io.github.lishangbu.gamedata.entity.backAssetKey
import io.github.lishangbu.gamedata.entity.code
import io.github.lishangbu.gamedata.entity.contentPackId
import io.github.lishangbu.gamedata.entity.creatureId
import io.github.lishangbu.gamedata.entity.defaultSkin
import io.github.lishangbu.gamedata.entity.enabled
import io.github.lishangbu.gamedata.entity.flavorText
import io.github.lishangbu.gamedata.entity.frontAssetKey
import io.github.lishangbu.gamedata.entity.genus
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.entity.itemId
import io.github.lishangbu.gamedata.entity.name
import io.github.lishangbu.gamedata.entity.speciesId
import io.github.lishangbu.gamedata.entity.skillId
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.ilike
import org.babyfish.jimmer.sql.kt.ast.expression.or
import org.babyfish.jimmer.sql.kt.ast.expression.valueIn
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 以批量查询聚合玩家可读资料，避免向玩家暴露管理端实体与权限。 */
@Service
class PlayerCatalogService(
	private val sqlClient: KSqlClient,
	private val contentPacks: PublishedContentPackService,
) {
	@Transactional(readOnly = true)
	fun creatures(page: Int, size: Int, query: String?): PlayerCatalogPage<PlayerCatalogCreatureResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		val contentPackId = contentPacks.requireId()
		val creatures = sqlClient.createQuery(GameCreature::class) {
			where(table.contentPackId eq contentPackId, table.enabled eq true)
			search.pattern?.let { pattern -> where(or(table.code ilike pattern, table.name ilike pattern)) }
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size)
		if (creatures.rows.isEmpty()) return PlayerCatalogPage(emptyList(), creatures.totalRowCount)

		val speciesDetails = sqlClient.createQuery(GameSpeciesDetails::class) {
			where(table.speciesId valueIn creatures.rows.map(GameCreature::speciesId))
			select(table)
		}.execute().associateBy(GameSpeciesDetails::speciesId)
		val skins = sqlClient.createQuery(GameCreatureSkin::class) {
			where(
				table.creatureId valueIn creatures.rows.map(GameCreature::id),
				table.defaultSkin eq true,
				table.enabled eq true,
			)
			select(table)
		}.execute().associateBy(GameCreatureSkin::creatureId)

		return PlayerCatalogPage(
			items = creatures.rows.map { creature ->
				val detail = speciesDetails[creature.speciesId]
				val skin = checkNotNull(skins[creature.id]) { "启用 Creature 缺少启用默认 Skin: ${creature.code}" }
				PlayerCatalogCreatureResponse {
					id = creature.id
					code = creature.code
					name = creature.name
					genus = detail?.genus
					flavorText = detail?.flavorText
					height = creature.height
					weight = creature.weight
					defaultSkin = PlayerCatalogSkinResponse {
						id = skin.id
						code = skin.code
						name = skin.name
						avatarAssetKey = skin.avatarAssetKey
						frontAssetKey = skin.frontAssetKey
						backAssetKey = skin.backAssetKey
					}
				}
			},
			total = creatures.totalRowCount,
		)
	}

	@Transactional(readOnly = true)
	fun skills(page: Int, size: Int, query: String?): PlayerCatalogPage<PlayerCatalogSkillResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		val contentPackId = contentPacks.requireId()
		val skills = sqlClient.createQuery(GameSkill::class) {
			where(table.contentPackId eq contentPackId, table.enabled eq true)
			search.pattern?.let { pattern -> where(or(table.code ilike pattern, table.name ilike pattern)) }
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size)
		val details = if (skills.rows.isEmpty()) emptyMap() else sqlClient.createQuery(GameSkillDetails::class) {
			where(table.skillId valueIn skills.rows.map(GameSkill::id))
			select(table)
		}.execute().associateBy(GameSkillDetails::skillId)
		return PlayerCatalogPage(
			skills.rows.map { skill ->
				val detail = details[skill.id]
				PlayerCatalogSkillResponse {
					id = skill.id
					code = skill.code
					name = skill.name
					elementId = skill.elementId
					accuracy = skill.accuracy
					power = skill.power
					pp = skill.pp
					priority = skill.priority
					shortEffect = detail?.shortEffect
					effect = detail?.effect
					flavorText = detail?.flavorText
				}
			},
			skills.totalRowCount,
		)
	}

	@Transactional(readOnly = true)
	fun abilities(page: Int, size: Int, query: String?): PlayerCatalogPage<PlayerCatalogAbilityResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		val contentPackId = contentPacks.requireId()
		val abilities = sqlClient.createQuery(GameAbility::class) {
			where(table.contentPackId eq contentPackId, table.enabled eq true)
			search.pattern?.let { pattern -> where(or(table.code ilike pattern, table.name ilike pattern)) }
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size)
		val details = if (abilities.rows.isEmpty()) emptyMap() else sqlClient.createQuery(GameAbilityDetails::class) {
			where(table.abilityId valueIn abilities.rows.map(GameAbility::id))
			select(table)
		}.execute().associateBy(GameAbilityDetails::abilityId)
		return PlayerCatalogPage(
			abilities.rows.map { ability ->
				val detail = details[ability.id]
				PlayerCatalogAbilityResponse {
					id = ability.id
					code = ability.code
					name = ability.name
					shortEffect = detail?.shortEffect
					effect = detail?.effect
					flavorText = detail?.flavorText
				}
			},
			abilities.totalRowCount,
		)
	}

	@Transactional(readOnly = true)
	fun items(page: Int, size: Int, query: String?): PlayerCatalogPage<PlayerCatalogItemResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		val contentPackId = contentPacks.requireId()
		val items = sqlClient.createQuery(GameItem::class) {
			where(table.contentPackId eq contentPackId, table.enabled eq true)
			search.pattern?.let { pattern -> where(or(table.code ilike pattern, table.name ilike pattern)) }
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size)
		val details = if (items.rows.isEmpty()) emptyMap() else sqlClient.createQuery(GameItemDetails::class) {
			where(table.itemId valueIn items.rows.map(GameItem::id))
			select(table)
		}.execute().associateBy(GameItemDetails::itemId)
		return PlayerCatalogPage(
			items.rows.map { item ->
				val detail = details[item.id]
				PlayerCatalogItemResponse {
					id = item.id
					code = item.code
					name = item.name
					usageType = item.usageType
					iconAssetKey = item.iconAssetKey
					cost = item.cost
					shortEffect = detail?.shortEffect
					effect = detail?.effect
					flavorText = detail?.flavorText
				}
			},
			items.totalRowCount,
		)
	}
}
