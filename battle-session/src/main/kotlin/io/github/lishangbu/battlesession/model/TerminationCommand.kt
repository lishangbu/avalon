package io.github.lishangbu.battlesession.model

/** 以幂等 commandId 和期望 revision 终止仍为 ACTIVE 的会话。 */
data class TerminationCommand(
	val commandId: String,
	val expectedRevision: Long,
	val reason: String,
) {
	init {
		requireUuidV4(commandId, "commandId")
		require(expectedRevision >= 0) { "expectedRevision must not be negative" }
		require(reason.isNotBlank()) { "termination reason must not be blank" }
	}
}
