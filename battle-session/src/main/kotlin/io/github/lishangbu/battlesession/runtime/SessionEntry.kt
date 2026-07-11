package io.github.lishangbu.battlesession.runtime

import io.github.lishangbu.battleengine.model.BattleInitialState
import io.github.lishangbu.battlesession.model.BattleSessionSnapshot
import io.github.lishangbu.battlesession.model.TerminationCommand
import io.github.lishangbu.battlesession.model.TerminationResult
import io.github.lishangbu.battlesession.model.TurnCommand
import io.github.lishangbu.battlesession.model.TurnRecord

/**
 * 独占单个会话的可变运行态、串行锁与幂等命令索引。
 *
 * Snapshot 只在 [lock] 内推进；Runtime 注册表只负责定位本实例，不直接修改会话状态。
 */
internal class SessionEntry(
	var snapshot: BattleSessionSnapshot,
	val initialState: BattleInitialState,
) {
	val lock = Any()
	val initialEvents = snapshot.state.events
	val commandCache = mutableMapOf<String, CachedCommand>()

	/** 统一约束会话内可幂等重放的命令缓存项。 */
	sealed interface CachedCommand

	/** 保存回合命令原始负载及其已提交事实。 */
	data class CachedTurnCommand(
		val command: TurnCommand,
		val turnRecord: TurnRecord,
	) : CachedCommand

	/** 保存终止命令原始负载及其不可变结果。 */
	data class CachedTerminationCommand(
		val command: TerminationCommand,
		val result: TerminationResult,
	) : CachedCommand
}
