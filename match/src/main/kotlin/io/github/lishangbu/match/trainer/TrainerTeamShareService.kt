package io.github.lishangbu.match.trainer

import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.util.Base64

/** 创建冻结的 Team 分享文档，并将其重新校验后导入为独立副本。 */
open class TrainerTeamShareService(
	private val repository: MatchTrainerTeamShareRepository,
	private val teams: TrainerTeamService,
	private val sqlClient: KSqlClient,
	private val random: SecureRandom = SecureRandom(),
) {
	@Transactional
	open fun share(trainerId: Long, teamId: Long): TrainerTeamShareResponse {
		val team = teams.get(trainerId, teamId)
		val code = generateCode()
		val saved = repository.save(MatchTrainerTeamShare {
			this.teamId = team.id
			this.code = code
			teamRevision = team.revision
			snapshot = TrainerTeamShareSnapshot(
				name = team.name,
				members = team.members.map { it.toShareMember() },
			)
		}, SaveMode.INSERT_ONLY)
		return TrainerTeamShareResponse(saved.code, saved.teamRevision)
	}

	@Transactional
	open fun import(trainerId: Long, request: ImportTrainerTeamRequest): TrainerTeamRecord {
		if (!SHARE_CODE.matches(request.shareCode)) throw TrainerTeamRequestException("trainer-team.share.not-found")
		val share = sqlClient.createQuery(MatchTrainerTeamShare::class) {
			where(table.code eq request.shareCode)
			select(table)
		}.execute().singleOrNull() ?: throw TrainerTeamRequestException("trainer-team.share.not-found")
		val name = request.name ?: "${share.snapshot.name} 副本"
		return teams.create(trainerId, SaveTrainerTeamRequest(
			name = name,
			members = share.snapshot.members.map { it.toSaveRequest() },
		))
	}

	private fun generateCode(): String {
		repeat(4) {
			val bytes = ByteArray(16).also(random::nextBytes)
			val candidate = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
			val exists = sqlClient.createQuery(MatchTrainerTeamShare::class) {
				where(table.code eq candidate)
				select(table.id)
			}.execute().isNotEmpty()
			if (!exists) return candidate
		}
		throw TrainerTeamRequestException("trainer-team.share.unavailable")
	}

	private fun TrainerTeamMemberRecord.toShareMember() = TrainerTeamShareSnapshotMember(
		creatureId, gender, skinId, skillIds, abilityId, itemId, natureId, teraElementId, individualValues, effortValues,
	)

	private fun TrainerTeamShareSnapshotMember.toSaveRequest() = SaveTrainerTeamMemberRequest(
		creatureId = creatureId.toString(),
		gender = gender,
		skinId = skinId.toString(),
		skillIds = skillIds.map(Long::toString),
		abilityId = abilityId.toString(),
		itemId = itemId.toString(),
		natureId = natureId.toString(),
		teraElementId = teraElementId.toString(),
		individualValues = individualValues,
		effortValues = effortValues,
	)

	private companion object {
		val SHARE_CODE = Regex("^[A-Za-z0-9_-]{22}$")
	}
}
