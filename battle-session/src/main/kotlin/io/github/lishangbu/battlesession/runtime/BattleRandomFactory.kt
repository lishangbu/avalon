package io.github.lishangbu.battlesession.runtime

import io.github.lishangbu.battleengine.random.BattleRandom
import io.github.lishangbu.battleengine.random.SecureBattleRandom

/** 为每个成功执行的 Turn Command 提供独立服务端随机源。 */
internal fun interface BattleRandomFactory {
	fun create(): BattleRandom

	/** 使用安全随机源，避免调用方通过可预测种子影响战斗结果。 */
	class Secure : BattleRandomFactory {
		override fun create(): BattleRandom = SecureBattleRandom()
	}
}
