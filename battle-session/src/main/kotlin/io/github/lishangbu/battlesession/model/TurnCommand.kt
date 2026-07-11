package io.github.lishangbu.battlesession.model

import io.github.lishangbu.battleengine.model.BattleAction
import java.util.UUID

/**
 * 请求会话恰好推进一个完整回合。
 *
 * commandId 标识网络重试，expectedRevision 防止基于过期 Snapshot 提交行动。
 */
data class TurnCommand(
	val commandId: String,
	val expectedRevision: Long,
	val actions: List<BattleAction>,
) {
	init {
		requireUuidV4(commandId, "commandId")
		require(expectedRevision >= 0) { "expectedRevision must not be negative" }
	}
}

internal fun requireUuidV4(value: String, fieldName: String) {
	val uuid = runCatching { UUID.fromString(value) }
		.getOrElse { throw IllegalArgumentException("$fieldName must be a UUID v4", it) }
	require(uuid.version() == 4) { "$fieldName must be a UUID v4" }
}
