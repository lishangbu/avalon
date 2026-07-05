package io.github.lishangbu.battleengine.random

import java.security.SecureRandom
import java.util.random.RandomGenerator

/**
 * 生产对战默认随机源。
 *
 * 战斗引擎本身只依赖 [BattleRandom]，不会主动选择随机算法；测试、replay 和沙盒各自有脚本、trace 或种子随机源。
 * 真实对战入口如果没有显式随机实现，应优先使用本类，而不是复用那些调试随机源。默认构造使用 JDK
 * [SecureRandom]，让命中、伤害浮动、同速排序和随机目标等关键分支不依赖可预测的线性伪随机序列。
 *
 * 构造器允许传入 [RandomGenerator] 只服务测试或离线模拟：调用方可以固定种子验证边界，但生产代码应使用默认
 * 构造。若需要保存可复盘材料，请在外层包一层 [RecordingBattleRandom]；本类只负责产生随机值，不负责记录
 * `reason`，避免把审计职责和随机算法绑死在一起。
 */
class SecureBattleRandom(
	private val random: RandomGenerator = SecureRandom(),
) : BattleRandom {
	override fun nextInt(bound: Int, reason: String): Int {
		require(bound > 0) { "bound must be positive" }
		return random.nextInt(bound)
	}
}
