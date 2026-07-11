package io.github.lishangbu.battlesession

/** 表示同一 commandId 被重复用于不同命令负载。 */
class CommandPayloadConflictException(
	val commandId: String,
) : IllegalStateException("commandId was already used with a different payload: $commandId")
