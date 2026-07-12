package io.github.lishangbu.match.trainer

import io.github.lishangbu.gamedata.entity.GameCreature
import io.github.lishangbu.gamedata.entity.GameCreatureAbility
import io.github.lishangbu.gamedata.entity.GameCreatureSkillLearns
import io.github.lishangbu.gamedata.entity.GameAbility
import io.github.lishangbu.gamedata.entity.GameItem
import io.github.lishangbu.gamedata.entity.GameNatures
import io.github.lishangbu.gamedata.entity.GameSkill
import io.github.lishangbu.gamedata.entity.abilityId as gameAbilityId
import io.github.lishangbu.gamedata.entity.creatureId as gameCreatureId
import io.github.lishangbu.gamedata.entity.enabled as gameDataEnabled
import io.github.lishangbu.gamedata.entity.id as gameDataId
import io.github.lishangbu.gamedata.entity.increasedStatId as natureIncreasedStatId
import io.github.lishangbu.gamedata.entity.decreasedStatId as natureDecreasedStatId
import io.github.lishangbu.gamedata.entity.skillId as gameSkillId
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.isNull
import org.babyfish.jimmer.sql.kt.ast.expression.valueIn
import org.springframework.transaction.annotation.Transactional

/**
 * 以完整替换语义保存当前 Trainer 的唯一 Team。
 *
 * 聚合根 revision 与全部成员、技能槽位在同一事务提交，任何校验或写入失败都不会留下草稿阵容。
 */
open class TrainerTeamService(
	private val teams: MatchTrainerTeamRepository,
	private val members: MatchTrainerTeamMemberRepository,
	private val skills: MatchTrainerTeamMemberSkillRepository,
	private val sqlClient: KSqlClient,
) {
	/** Match 冻结或接受旧 Snapshot 前，按 Current Game Data 重新验证全部引用。 */
	internal open fun validateForMatch(members: List<TrainerTeamMemberRecord>) = validateReferences(members)

	open fun find(trainerId: Long): TrainerTeamRecord? {
		repeat(3) {
			val team = findTeam(trainerId) ?: return null
			val teamMembers = loadMembers(team.id)
			val confirmedRevision = findTeam(trainerId)?.revision
			if (confirmedRevision == team.revision) return team.toRecord(teamMembers)
		}
		throw TrainerTeamRequestException("trainer-team.concurrent-update")
	}

	@Transactional
	open fun save(trainerId: Long, request: SaveTrainerTeamRequest): TrainerTeamRecord {
		val normalized = normalize(request.members, defaultNatureId(request.members))
		validateReferences(normalized)
		lockTrainer(trainerId)
		val existing = findTeam(trainerId)
		val team = if (existing == null) {
			if (request.expectedRevision != null) throw TrainerTeamRequestException("trainer-team.revision-conflict")
			teams.save(MatchTrainerTeam {
				this.trainerId = trainerId
				revision = 0L
			}, SaveMode.INSERT_ONLY)
		} else {
			if (request.expectedRevision != existing.revision) throw TrainerTeamRequestException("trainer-team.revision-conflict")
			val changed = sqlClient.createUpdate(MatchTrainerTeam::class) {
				where(table.id eq existing.id, table.revision eq existing.revision)
				set(table.revision, existing.revision + 1)
			}.execute()
			if (changed != 1) throw TrainerTeamRequestException("trainer-team.revision-conflict")
			findTeam(trainerId) ?: throw TrainerTeamRequestException("trainer-team.unavailable")
		}

		sqlClient.createDelete(MatchTrainerTeamMember::class) { where(table.teamId eq team.id) }.execute()
		normalized.forEachIndexed { index, member ->
			val savedMember = members.save(MatchTrainerTeamMember {
				teamId = team.id
				position = index + 1
				creatureId = member.creatureId
				abilityId = member.abilityId
				itemId = member.itemId
				natureId = member.natureId
				individualValues = member.individualValues
				effortValues = member.effortValues
			}, SaveMode.INSERT_ONLY)
			member.skillIds.forEachIndexed { skillIndex, skillId ->
				skills.save(MatchTrainerTeamMemberSkill {
					id = MatchTrainerTeamMemberSkillId {
						teamMemberId = savedMember.id
						position = skillIndex + 1
					}
					this.skillId = skillId
				}, SaveMode.INSERT_ONLY)
			}
		}
		return find(trainerId) ?: throw TrainerTeamRequestException("trainer-team.unavailable")
	}

	private fun findTeam(trainerId: Long): MatchTrainerTeam? = sqlClient.createQuery(MatchTrainerTeam::class) {
		where(table.trainerId eq trainerId)
		select(table)
	}.execute().singleOrNull()

	/** 同一 Trainer 的首次创建与后续完整替换共享一把数据库行锁，唯一键竞争统一收敛为 revision 冲突。 */
	private fun lockTrainer(trainerId: Long) {
		val found = sqlClient.createQuery(MatchTrainer::class) {
			where(table.id eq trainerId, table.archivedAt.isNull())
			select(table.id)
		}.forUpdate().execute().isNotEmpty()
		if (!found) throw TrainerTeamRequestException("trainer-team.unavailable")
	}

	private fun loadMembers(teamId: Long): List<TrainerTeamMemberRecord> {
		val memberRows = sqlClient.createQuery(MatchTrainerTeamMember::class) {
		where(table.teamId eq teamId)
		orderBy(table.position)
		select(table)
		}.execute()
		if (memberRows.isEmpty()) return emptyList()
		val skillIdsByMember = sqlClient.createQuery(MatchTrainerTeamMemberSkill::class) {
			where(table.id.teamMemberId valueIn memberRows.map(MatchTrainerTeamMember::id))
			orderBy(table.id.teamMemberId, table.id.position)
			select(table)
		}.execute().groupBy({ it.id.teamMemberId }, { it.skillId })
		return memberRows.map { member ->
		TrainerTeamMemberRecord(
			member.creatureId, skillIdsByMember[member.id].orEmpty(), member.abilityId, member.itemId, member.natureId,
			member.individualValues, member.effortValues,
		)
	}
	}

	private fun normalize(requests: List<SaveTrainerTeamMemberRequest>, defaultNatureId: Long): List<TrainerTeamMemberRecord> {
		if (requests.size !in 1..6) throw TrainerTeamRequestException("trainer-team.members.invalid")
		return requests.map { request ->
			if (request.skillIds.size !in 1..4) throw TrainerTeamRequestException("trainer-team.skills.invalid")
			val ids = listOf(request.creatureId, request.abilityId, request.itemId, request.natureId ?: defaultNatureId.toString()) + request.skillIds
			val parsed = ids.map { it.toLongOrNull()?.takeIf { id -> id > 0 } ?: throw TrainerTeamRequestException("trainer-team.identifier.invalid") }
			val skillIds = parsed.drop(4)
			if (skillIds.toSet().size != skillIds.size) throw TrainerTeamRequestException("trainer-team.skills.invalid")
			val individualValues = normalizeStats(request.individualValues, 31, 0..31, "trainer-team.iv.invalid")
			val effortValues = normalizeStats(request.effortValues, 0, 0..252, "trainer-team.ev.invalid")
			if (effortValues.values.sum() > 510) throw TrainerTeamRequestException("trainer-team.ev.invalid")
			TrainerTeamMemberRecord(parsed[0], skillIds, parsed[1], parsed[2], parsed[3], individualValues, effortValues)
		}
	}

	/** 未指定 nature 时选择启用的中性 nature，而不依赖资料库主键常量。 */
	private fun defaultNatureId(requests: List<SaveTrainerTeamMemberRequest>): Long {
		if (requests.none { it.natureId == null }) return 0
		return sqlClient.createQuery(GameNatures::class) {
			where(table.gameDataEnabled eq true, table.natureIncreasedStatId.isNull(), table.natureDecreasedStatId.isNull())
			orderBy(table.gameDataId)
			select(table.gameDataId)
		}.execute().firstOrNull() ?: throw TrainerTeamRequestException("trainer-team.reference.invalid")
	}

	private fun normalizeStats(values: Map<String, Int>, default: Int, range: IntRange, code: String): Map<String, Int> {
		if (values.keys.any { it !in STAT_CODES }) throw TrainerTeamRequestException(code)
		return STAT_CODES.associateWith { codeName -> values[codeName]?.takeIf { it in range } ?: if (codeName in values) throw TrainerTeamRequestException(code) else default }
	}

	/**
	 * 资料表没有被 Match schema 外键绑定，写聚合前必须显式验证引用及 creature 的能力边界。
	 * 校验发生在首次写入之前，从而保证失败请求不会留下 Team 草稿。
	 */
	private fun validateReferences(teamMembers: List<TrainerTeamMemberRecord>) {
		val creatureIds = teamMembers.map(TrainerTeamMemberRecord::creatureId).toSet()
		val itemIds = teamMembers.map(TrainerTeamMemberRecord::itemId).toSet()
		val natureIds = teamMembers.map(TrainerTeamMemberRecord::natureId).toSet()
		val validCreatureIds = sqlClient.createQuery(GameCreature::class) {
			where(table.gameDataId valueIn creatureIds, table.gameDataEnabled eq true)
			select(table.gameDataId)
		}.execute().toSet()
		val validAbilityPairs = sqlClient.createQuery(GameCreatureAbility::class) {
			where(table.gameCreatureId valueIn creatureIds, table.gameAbilityId valueIn teamMembers.map(TrainerTeamMemberRecord::abilityId))
			select(table.gameCreatureId, table.gameAbilityId)
		}.execute().map { row -> row._1 to row._2 }.toSet()
		val enabledAbilityIds = sqlClient.createQuery(GameAbility::class) {
			where(table.gameDataId valueIn teamMembers.map(TrainerTeamMemberRecord::abilityId), table.gameDataEnabled eq true)
			select(table.gameDataId)
		}.execute().toSet()
		val validSkillPairs = sqlClient.createQuery(GameCreatureSkillLearns::class) {
			where(table.gameCreatureId valueIn creatureIds, table.gameSkillId valueIn teamMembers.flatMap(TrainerTeamMemberRecord::skillIds))
			select(table.gameCreatureId, table.gameSkillId)
		}.execute().map { row -> row._1 to row._2 }.toSet()
		val enabledSkillIds = sqlClient.createQuery(GameSkill::class) {
			where(table.gameDataId valueIn teamMembers.flatMap(TrainerTeamMemberRecord::skillIds), table.gameDataEnabled eq true)
			select(table.gameDataId)
		}.execute().toSet()
		val validItemIds = sqlClient.createQuery(GameItem::class) {
			where(table.gameDataId valueIn itemIds, table.gameDataEnabled eq true)
			select(table.gameDataId)
		}.execute().toSet()
		val validNatureIds = sqlClient.createQuery(GameNatures::class) {
			where(table.gameDataId valueIn natureIds, table.gameDataEnabled eq true)
			select(table.gameDataId)
		}.execute().toSet()
		teamMembers.forEach { member ->
			val skillsValid = member.skillIds.all { skillId -> skillId in enabledSkillIds && member.creatureId to skillId in validSkillPairs }
			if (member.creatureId !in validCreatureIds || member.abilityId !in enabledAbilityIds ||
				member.creatureId to member.abilityId !in validAbilityPairs ||
				!skillsValid || member.itemId !in validItemIds || member.natureId !in validNatureIds) {
				throw TrainerTeamRequestException("trainer-team.reference.invalid")
			}
		}
	}

	private fun MatchTrainerTeam.toRecord(members: List<TrainerTeamMemberRecord>) = TrainerTeamRecord(id, trainerId, revision, members)

	private companion object {
		val STAT_CODES = listOf("hp", "attack", "defense", "special-attack", "special-defense", "speed")
	}
}
