package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battlerules.dto.BattleActionRequest
import io.github.lishangbu.battlerules.dto.BattleSessionResponse
import io.github.lishangbu.battlerules.dto.BattleSessionSummaryResponse
import io.github.lishangbu.battlerules.dto.BattleSessionTurnRecordResponse
import io.github.lishangbu.battlesession.model.BattleSessionSnapshot
import io.github.lishangbu.battlesession.model.BattleSessionSummary
import io.github.lishangbu.battlesession.model.TurnRecord
import org.springframework.stereotype.Component
import kotlin.reflect.full.memberProperties

/** 将纯 Kotlin Session Runtime 快照适配为稳定的管理端 DTO。 */
@Component
class BattleSessionResponseMapper {
	fun toTurnRecord(record: TurnRecord): BattleSessionTurnRecordResponse =
		BattleSessionTurnRecordResponse(
			commandId = record.commandId,
			revisionBefore = record.revisionBefore,
			revisionAfter = record.revisionAfter,
			turnNumber = record.turnNumber,
			submittedActions = record.submittedActions.map(::toActionRequest),
			randomTrace = record.randomTrace.map { trace ->
				BattleSessionTurnRecordResponse.RandomTrace(
					sequence = trace.sequence,
					bound = trace.bound,
					reason = trace.reason,
					value = trace.value,
				)
			},
			events = record.events.map(::toEvent),
			resolvedAt = record.resolvedAt,
		)

	fun toSummary(summary: BattleSessionSummary): BattleSessionSummaryResponse =
		BattleSessionSummaryResponse(
			sessionId = summary.sessionId,
			formatCode = summary.formatCode,
			status = summary.status.name,
			revision = summary.revision,
			turnNumber = summary.turnNumber,
			createdAt = summary.createdAt,
			updatedAt = summary.updatedAt,
			endedAt = summary.endedAt,
			expiresAt = summary.expiresAt,
			result = summary.result?.let { result ->
				BattleSessionResponse.Result(
					winningSideId = result.winningSideId,
					reason = result.reason,
				)
			},
			terminationReason = summary.terminationReason,
		)

	fun toResponse(snapshot: BattleSessionSnapshot): BattleSessionResponse =
		BattleSessionResponse(
			sessionId = snapshot.sessionId,
			formatCode = snapshot.formatCode,
			status = snapshot.status.name,
			revision = snapshot.revision,
			turnNumber = snapshot.state.turnNumber,
			result = snapshot.state.result?.let { result ->
				BattleSessionResponse.Result(
					winningSideId = result.winningSideId,
					reason = result.reason,
				)
			},
			termination = snapshot.battleRecord?.termination?.let { termination ->
				BattleSessionResponse.Termination(
					commandId = termination.commandId,
					reason = termination.reason,
					revisionBefore = termination.revisionBefore,
					revisionAfter = termination.revisionAfter,
					terminatedAt = termination.terminatedAt,
				)
			},
			createdAt = snapshot.createdAt,
			updatedAt = snapshot.updatedAt,
			endedAt = snapshot.endedAt,
			expiresAt = snapshot.expiresAt,
			sides = snapshot.state.sides.map { side ->
				BattleSessionResponse.Side(
					sideId = side.sideId,
					activeActorIds = side.activeActorIds,
					participants = side.participants.map { participant ->
						BattleSessionResponse.Participant(
							actorId = participant.actorId,
							creatureId = participant.creatureId.toString(),
							active = participant.actorId in side.activeActorIds,
							level = participant.level,
							currentHp = participant.currentHp,
							maxHp = participant.maxHp,
							majorStatus = participant.majorStatus?.name,
							statStages = participant.statStages.mapKeys { (stat, _) -> stat.name },
							skillSlots = participant.skillSlots.map { slot ->
								BattleSessionResponse.SkillSlot(
									skillId = slot.skillId.toString(),
									name = slot.name,
									remainingPp = slot.remainingPp,
									maxPp = slot.maxPp,
								)
							},
						)
					},
				)
			},
			turnRequirements = snapshot.requirements.selections.map { requirement ->
				BattleSessionResponse.TurnRequirement(
					actorId = requirement.actorId,
					options = requirement.options.map(::toActionRequest),
				)
			},
		)

	fun toActionRequest(action: BattleAction): BattleActionRequest =
		when (action) {
			is BattleAction.UseSkill -> BattleActionRequest(
				type = "USE_SKILL",
				actorId = action.actorId,
				skillId = action.skillId,
				targetActorId = action.targetActorId,
			)

			is BattleAction.SwitchParticipant -> BattleActionRequest(
				type = "SWITCH_PARTICIPANT",
				actorId = action.actorId,
				skillId = null,
				targetActorId = action.targetActorId,
			)
		}

	internal fun toEvent(event: BattleEvent): BattleSessionTurnRecordResponse.Event =
		BattleSessionTurnRecordResponse.Event(
			type = event::class.simpleName ?: "BattleEvent",
			turnNumber = event.turnNumber,
			payload = event::class.memberProperties
				.filterNot { property -> property.name == "turnNumber" }
				.associate { property ->
					property.name to property.getter.call(event).toPayloadValue(property.name.isIdentifierName())
				},
		)

	private fun Any?.toPayloadValue(identifier: Boolean = false): Any? =
		when (this) {
			null, is String, is Boolean -> this
			is Number -> if (identifier) toString() else this
			is Enum<*> -> name
			is Iterable<*> -> map { value -> value.toPayloadValue(identifier) }
			is Map<*, *> -> entries.associate { (key, value) ->
				val propertyName = key.toString()
				propertyName to value.toPayloadValue(propertyName.isIdentifierName())
			}
			else -> toString()
		}

	private fun String.isIdentifierName(): Boolean =
		this == "id" || endsWith("_id") || endsWith("_ids") || endsWith("Id") || endsWith("Ids")
}
