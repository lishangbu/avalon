package io.github.lishangbu.battlesession

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
