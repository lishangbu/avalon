package io.github.lishangbu.match.game

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battlesession.model.BattleSessionSnapshot
import io.github.lishangbu.match.challenge.ChallengeRequestException
import io.github.lishangbu.match.challenge.TrainerTeamSnapshotMember
import io.github.lishangbu.match.challenge.TrainerTeamSnapshotRoster
import io.github.lishangbu.match.trainer.TrainerTeamMemberRecord
import io.github.lishangbu.match.trainer.TrainerTeamRecord
import org.springframework.http.HttpStatus
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.UUID

internal fun TrainerTeamRecord.toSnapshot(lead: Int) = TrainerTeamSnapshotRoster(lead, members.map {
	TrainerTeamSnapshotMember(
		creatureId = it.creatureId,
		skinId = it.skinId,
		skillIds = it.skillIds,
		abilityId = it.abilityId,
		itemId = it.itemId,
		natureId = it.natureId,
		teraElementId = it.teraElementId,
		level = 50,
		individualValues = it.individualValues,
		effortValues = it.effortValues,
	)
})

internal fun TrainerTeamSnapshotMember.toTeamMember() = TrainerTeamMemberRecord(
	creatureId, skinId, skillIds, abilityId, itemId, natureId, teraElementId, individualValues, effortValues,
)

internal fun MatchBattleViewOption.toViewOption(viewerSide: Int) =
	MatchTurnOptionResponse {
		type = this@toViewOption.type
		skillId = this@toViewOption.skillId
		targetPosition = this@toViewOption.targetPosition
		targetYou = targetSide == viewerSide
		canTerastallize = this@toViewOption.canTerastallize
	}

internal fun BattleSessionSnapshot.toViewState() = MatchBattleViewState(
	sides = state.sides.map { side ->
		MatchBattleViewSide(side.participants.map { member ->
		MatchBattleViewParticipant(member.creatureId, member.actorId in side.activeActorIds, member.currentHp, member.maxHp)
			.copy(teraElementId = member.teraElementId.takeIf { member.terastallized })
		})
	},
	requirements = requirements.selections.map { requirement ->
		MatchBattleViewRequirement(
			actorSide = actorSide(requirement.actorId),
			actorPosition = actorPosition(requirement.actorId),
			options = requirement.options.map { option ->
				val targetActorId = when (option) {
					is BattleAction.UseSkill -> option.targetActorId
					is BattleAction.SwitchParticipant -> option.targetActorId
				}
				MatchBattleViewOption(
					type = if (option is BattleAction.UseSkill) "USE_SKILL" else "SWITCH_PARTICIPANT",
					skillId = (option as? BattleAction.UseSkill)?.skillId,
					targetSide = actorSide(targetActorId),
					targetPosition = actorPosition(targetActorId),
					canTerastallize = option is BattleAction.UseSkill &&
						state.participant(requirement.actorId)?.teraElementId != null &&
						state.participant(requirement.actorId)?.terastallized == false &&
						state.sideOf(requirement.actorId)?.terastallizationUsed == false,
				)
			},
		)
	},
)

internal fun actorPosition(actorId: String): Int = actorId.substringAfterLast('-').toInt()
internal fun actorSide(actorId: String): Int = actorId.substringAfter("side-").substringBefore('-').toInt()

internal fun BattleAction.matchesRequirement(option: BattleAction): Boolean = when {
	this is BattleAction.UseSkill && option is BattleAction.UseSkill ->
		actorId == option.actorId && skillId == option.skillId && targetActorId == option.targetActorId
	this is BattleAction.SwitchParticipant && option is BattleAction.SwitchParticipant -> this == option
	else -> false
}

internal fun MatchTurnAction.toBattleAction(side: Int): BattleAction {
	val actorId = "side-$side-actor-$actorPosition"
	val targetSide = if (targetYou) side else 3 - side
	val targetActorId = "side-$targetSide-actor-$targetPosition"
	return when (type) {
		"USE_SKILL" -> BattleAction.UseSkill(
			actorId,
			skillId ?: throw ChallengeRequestException(HttpStatus.UNPROCESSABLE_ENTITY, "match.turn.invalid"),
			targetActorId,
			terastallize,
		)
		"SWITCH_PARTICIPANT" -> BattleAction.SwitchParticipant(actorId, targetActorId)
		else -> throw ChallengeRequestException(HttpStatus.UNPROCESSABLE_ENTITY, "match.turn.invalid")
	}
}

internal fun String.toUuidV4OrNull(): UUID? =
	runCatching { UUID.fromString(this) }.getOrNull()?.takeIf { it.version() == 4 }

internal fun stableTurnCommandId(matchId: Long, turnNumber: Int): String {
	val digest = MessageDigest.getInstance("SHA-256").digest("$matchId:$turnNumber".toByteArray())
	digest[6] = ((digest[6].toInt() and 0x0f) or 0x40).toByte()
	digest[8] = ((digest[8].toInt() and 0x3f) or 0x80).toByte()
	val bytes = ByteBuffer.wrap(digest)
	return UUID(bytes.long, bytes.long).toString()
}
