package io.github.lishangbu.match.trainer

import io.github.lishangbu.battleengine.model.BattleGender
import io.github.lishangbu.gamedata.entity.GameAbility
import io.github.lishangbu.gamedata.entity.GameCreature
import io.github.lishangbu.gamedata.entity.GameCreatureAbility
import io.github.lishangbu.gamedata.entity.GameCreatureSkillLearns
import io.github.lishangbu.gamedata.entity.GameCreatureSkin
import io.github.lishangbu.gamedata.entity.GameElement
import io.github.lishangbu.gamedata.entity.GameItem
import io.github.lishangbu.gamedata.entity.GameNatures
import io.github.lishangbu.gamedata.entity.GameSkill
import io.github.lishangbu.gamedata.entity.GameSpecies
import io.github.lishangbu.gamedata.entity.ItemUsageType
import io.github.lishangbu.gamedata.entity.abilityId as gameAbilityId
import io.github.lishangbu.gamedata.entity.creatureId as gameCreatureId
import io.github.lishangbu.gamedata.entity.decreasedStatId as natureDecreasedStatId
import io.github.lishangbu.gamedata.entity.enabled as gameDataEnabled
import io.github.lishangbu.gamedata.entity.genderRate as gameSpeciesGenderRate
import io.github.lishangbu.gamedata.entity.id as gameDataId
import io.github.lishangbu.gamedata.entity.increasedStatId as natureIncreasedStatId
import io.github.lishangbu.gamedata.entity.skillId as gameSkillId
import io.github.lishangbu.gamedata.entity.speciesId as gameCreatureSpeciesId
import io.github.lishangbu.gamedata.entity.usageType as gameItemUsageType
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.isNull
import org.babyfish.jimmer.sql.kt.ast.expression.valueIn
import org.springframework.transaction.annotation.Transactional
import java.util.Locale

/** 管理一个 Trainer 最多二十支命名 Team，并完整替换各 Team 的阵容。 */
open class TrainerTeamService(
	private val teams: MatchTrainerTeamRepository,
	private val members: MatchTrainerTeamMemberRepository,
	private val skills: MatchTrainerTeamMemberSkillRepository,
	private val sqlClient: KSqlClient,
) {
	/** Match 冻结 Snapshot 前，按 Current Game Data 重新验证全部引用。 */
	internal open fun validateForMatch(members: List<TrainerTeamMemberRecord>) = validateReferences(members)

	open fun list(trainerId: Long): List<TrainerTeamRecord> = findTeams(trainerId).map { it.toRecord(loadMembers(it.id)) }

	open fun get(trainerId: Long, teamId: Long): TrainerTeamRecord =
		findConsistent(trainerId, teamId) ?: throw TrainerTeamRequestException("trainer-team.not-found")

	/** 兼容 Match/Challenge 的临时 seam：始终读取 Trainer 当前激活 Team。 */
	open fun find(trainerId: Long): TrainerTeamRecord? = findActive(trainerId)

	open fun findActive(trainerId: Long): TrainerTeamRecord? {
		val team = sqlClient.createQuery(MatchTrainerTeam::class) {
			where(table.trainerId eq trainerId, table.active eq true)
			select(table)
		}.execute().singleOrNull() ?: return null
		return findConsistent(trainerId, team.id)
	}

	@Transactional
	open fun create(trainerId: Long, request: SaveTrainerTeamRequest): TrainerTeamRecord {
		val (name, nameKey) = normalizeName(request.name)
		val normalized = normalize(request.members, defaultNatureId(request.members))
		validateReferences(normalized)
		lockTrainer(trainerId)
		val existing = findTeams(trainerId)
		if (existing.size >= MAX_TEAMS) throw TrainerTeamRequestException("trainer-team.limit-exceeded")
		ensureNameAvailable(trainerId, nameKey, null)
		if (request.expectedRevision != null) throw TrainerTeamRequestException("trainer-team.revision-conflict")
		val team = teams.save(MatchTrainerTeam {
			this.trainerId = trainerId
			this.name = name
			this.nameKey = nameKey
			active = existing.isEmpty()
			revision = 0L
		}, SaveMode.INSERT_ONLY)
		writeMembers(team.id, normalized)
		return findConsistent(trainerId, team.id) ?: throw TrainerTeamRequestException("trainer-team.unavailable")
	}

	@Transactional
	open fun update(trainerId: Long, teamId: Long, request: SaveTrainerTeamRequest): TrainerTeamRecord {
		val (name, nameKey) = normalizeName(request.name)
		val normalized = normalize(request.members, defaultNatureId(request.members))
		validateReferences(normalized)
		lockTrainer(trainerId)
		val existing = findTeam(trainerId, teamId) ?: throw TrainerTeamRequestException("trainer-team.not-found")
		if (request.expectedRevision != existing.revision) throw TrainerTeamRequestException("trainer-team.revision-conflict")
		ensureNameAvailable(trainerId, nameKey, teamId)
		val changed = sqlClient.createUpdate(MatchTrainerTeam::class) {
			where(table.id eq teamId, table.trainerId eq trainerId, table.revision eq existing.revision)
			set(table.name, name)
			set(table.nameKey, nameKey)
			set(table.revision, existing.revision + 1)
		}.execute()
		if (changed != 1) throw TrainerTeamRequestException("trainer-team.revision-conflict")
		writeMembers(teamId, normalized)
		return findConsistent(trainerId, teamId) ?: throw TrainerTeamRequestException("trainer-team.unavailable")
	}

	@Transactional
	open fun activate(trainerId: Long, teamId: Long): TrainerTeamRecord {
		lockTrainer(trainerId)
		val target = findTeam(trainerId, teamId) ?: throw TrainerTeamRequestException("trainer-team.not-found")
		if (!target.active) {
			sqlClient.createUpdate(MatchTrainerTeam::class) {
				where(table.trainerId eq trainerId, table.active eq true)
				set(table.active, false)
			}.execute()
			sqlClient.createUpdate(MatchTrainerTeam::class) {
				where(table.id eq teamId, table.trainerId eq trainerId)
				set(table.active, true)
			}.execute()
		}
		return findConsistent(trainerId, teamId) ?: throw TrainerTeamRequestException("trainer-team.unavailable")
	}

	private fun findConsistent(trainerId: Long, teamId: Long): TrainerTeamRecord? {
		repeat(3) {
			val team = findTeam(trainerId, teamId) ?: return null
			val teamMembers = loadMembers(team.id)
			val confirmedRevision = findTeam(trainerId, teamId)?.revision
			if (confirmedRevision == team.revision) return team.toRecord(teamMembers)
		}
		throw TrainerTeamRequestException("trainer-team.concurrent-update")
	}

	private fun findTeams(trainerId: Long): List<MatchTrainerTeam> = sqlClient.createQuery(MatchTrainerTeam::class) {
		where(table.trainerId eq trainerId)
		orderBy(table.id)
		select(table)
	}.execute()

	private fun findTeam(trainerId: Long, teamId: Long): MatchTrainerTeam? = sqlClient.createQuery(MatchTrainerTeam::class) {
		where(table.id eq teamId, table.trainerId eq trainerId)
		select(table)
	}.execute().singleOrNull()

	private fun ensureNameAvailable(trainerId: Long, nameKey: String, excludedTeamId: Long?) {
		val ids = sqlClient.createQuery(MatchTrainerTeam::class) {
			where(table.trainerId eq trainerId, table.nameKey eq nameKey)
			select(table.id)
		}.execute()
		if (ids.any { it != excludedTeamId }) throw TrainerTeamRequestException("trainer-team.name-conflict")
	}

	private fun writeMembers(teamId: Long, records: List<TrainerTeamMemberRecord>) {
		sqlClient.createDelete(MatchTrainerTeamMember::class) { where(table.teamId eq teamId) }.execute()
		records.forEachIndexed { index, member ->
			val savedMember = members.save(MatchTrainerTeamMember {
				this.teamId = teamId
				position = index + 1
				creatureId = member.creatureId
				gender = member.gender
				skinId = member.skinId
				abilityId = member.abilityId
				itemId = member.itemId
				natureId = member.natureId
				teraElementId = member.teraElementId
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
	}

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
				member.creatureId, member.gender, member.skinId, skillIdsByMember[member.id].orEmpty(), member.abilityId,
				member.itemId, member.natureId, member.teraElementId, member.individualValues, member.effortValues,
			)
		}
	}

	private fun normalize(requests: List<SaveTrainerTeamMemberRequest>, defaultNatureId: Long): List<TrainerTeamMemberRecord> {
		if (requests.size !in 1..6) throw TrainerTeamRequestException("trainer-team.members.invalid")
		return requests.map { request ->
			if (request.skillIds.size !in 1..4) throw TrainerTeamRequestException("trainer-team.skills.invalid")
			val skillIds = request.skillIds.map(::parseId)
			if (skillIds.toSet().size != skillIds.size) throw TrainerTeamRequestException("trainer-team.skills.invalid")
			val individualValues = normalizeStats(request.individualValues, 31, 0..31, "trainer-team.iv.invalid")
			val effortValues = normalizeStats(request.effortValues, 0, 0..252, "trainer-team.ev.invalid")
			if (effortValues.values.sum() > 510) throw TrainerTeamRequestException("trainer-team.ev.invalid")
			TrainerTeamMemberRecord(
				creatureId = parseId(request.creatureId),
				gender = request.gender ?: throw TrainerTeamRequestException("trainer-team.gender.invalid"),
				skinId = parseId(request.skinId),
				skillIds = skillIds,
				abilityId = parseId(request.abilityId),
				itemId = parseId(request.itemId),
				natureId = request.natureId?.let(::parseId) ?: defaultNatureId,
				teraElementId = parseId(request.teraElementId),
				individualValues = individualValues,
				effortValues = effortValues,
			)
		}
	}

	private fun parseId(value: String): Long = value.toLongOrNull()?.takeIf { it > 0 }
		?: throw TrainerTeamRequestException("trainer-team.identifier.invalid")

	private fun normalizeName(input: String): Pair<String, String> {
		val name = input.trim().replace(Regex("\\s+"), " ")
		if (name.isEmpty() || name.length > 40) throw TrainerTeamRequestException("trainer-team.name.invalid")
		return name to name.lowercase(Locale.ROOT)
	}

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
		return STAT_CODES.associateWith { stat ->
			values[stat]?.takeIf { it in range } ?: if (stat in values) throw TrainerTeamRequestException(code) else default
		}
	}

	private fun validateReferences(teamMembers: List<TrainerTeamMemberRecord>) {
		val creatureIds = teamMembers.map(TrainerTeamMemberRecord::creatureId).toSet()
		val speciesIdByCreatureId = sqlClient.createQuery(GameCreature::class) {
			where(table.gameDataId valueIn creatureIds, table.gameDataEnabled eq true)
			select(table.gameDataId, table.gameCreatureSpeciesId)
		}.execute().associate { it._1 to it._2 }
		val genderRateBySpeciesId = sqlClient.createQuery(GameSpecies::class) {
			where(table.gameDataId valueIn speciesIdByCreatureId.values.toSet(), table.gameDataEnabled eq true)
			select(table.gameDataId, table.gameSpeciesGenderRate)
		}.execute().associate { it._1 to it._2 }
		val skinsById = sqlClient.createQuery(GameCreatureSkin::class) {
			where(table.gameDataId valueIn teamMembers.map(TrainerTeamMemberRecord::skinId), table.gameDataEnabled eq true)
			select(table)
		}.execute().associate { it.id to it.creatureId }
		val validTeraElements = sqlClient.createQuery(GameElement::class) {
			where(table.gameDataId valueIn teamMembers.map(TrainerTeamMemberRecord::teraElementId), table.gameDataEnabled eq true)
			select(table.gameDataId)
		}.execute().toSet()
		val validAbilityPairs = sqlClient.createQuery(GameCreatureAbility::class) {
			where(table.gameCreatureId valueIn creatureIds, table.gameAbilityId valueIn teamMembers.map(TrainerTeamMemberRecord::abilityId))
			select(table.gameCreatureId, table.gameAbilityId)
		}.execute().map { it._1 to it._2 }.toSet()
		val enabledAbilityIds = sqlClient.createQuery(GameAbility::class) {
			where(table.gameDataId valueIn teamMembers.map(TrainerTeamMemberRecord::abilityId), table.gameDataEnabled eq true)
			select(table.gameDataId)
		}.execute().toSet()
		val validSkillPairs = sqlClient.createQuery(GameCreatureSkillLearns::class) {
			where(table.gameCreatureId valueIn creatureIds, table.gameSkillId valueIn teamMembers.flatMap(TrainerTeamMemberRecord::skillIds))
			select(table.gameCreatureId, table.gameSkillId)
		}.execute().map { it._1 to it._2 }.toSet()
		val enabledSkillIds = sqlClient.createQuery(GameSkill::class) {
			where(table.gameDataId valueIn teamMembers.flatMap(TrainerTeamMemberRecord::skillIds), table.gameDataEnabled eq true)
			select(table.gameDataId)
		}.execute().toSet()
		val validItemIds = sqlClient.createQuery(GameItem::class) {
			where(
				table.gameDataId valueIn teamMembers.map(TrainerTeamMemberRecord::itemId),
				table.gameDataEnabled eq true,
				table.gameItemUsageType eq ItemUsageType.HELD,
			)
			select(table.gameDataId)
		}.execute().toSet()
		val validNatureIds = sqlClient.createQuery(GameNatures::class) {
			where(table.gameDataId valueIn teamMembers.map(TrainerTeamMemberRecord::natureId), table.gameDataEnabled eq true)
			select(table.gameDataId)
		}.execute().toSet()
		teamMembers.forEach { member ->
			val skillsValid = member.skillIds.all { it in enabledSkillIds && member.creatureId to it in validSkillPairs }
			val genderValid = genderAllowed(
				member.gender,
				speciesIdByCreatureId[member.creatureId]?.let(genderRateBySpeciesId::get),
			)
			if (member.creatureId !in speciesIdByCreatureId || !genderValid || skinsById[member.skinId] != member.creatureId ||
				member.teraElementId !in validTeraElements || member.abilityId !in enabledAbilityIds ||
				member.creatureId to member.abilityId !in validAbilityPairs || !skillsValid ||
				member.itemId !in validItemIds || member.natureId !in validNatureIds) {
				throw TrainerTeamRequestException("trainer-team.reference.invalid")
			}
		}
	}

	/** 按 Current Game Data 的八分制性别比例约束队伍成员性别。 */
	private fun genderAllowed(gender: BattleGender, genderRate: Int?): Boolean = when (genderRate) {
		-1 -> gender == BattleGender.GENDERLESS
		0 -> gender == BattleGender.MALE
		8 -> gender == BattleGender.FEMALE
		in 1..7 -> gender == BattleGender.MALE || gender == BattleGender.FEMALE
		else -> false
	}

	private fun MatchTrainerTeam.toRecord(members: List<TrainerTeamMemberRecord>) =
		TrainerTeamRecord(id, trainerId, name, active, revision, members)

	private companion object {
		const val MAX_TEAMS = 20
		val STAT_CODES = listOf("hp", "attack", "defense", "special-attack", "special-defense", "speed")
	}
}
