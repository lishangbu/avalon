package io.github.lishangbu.avalon.battle.replay;

import io.github.lishangbu.avalon.battle.domain.BattleState;

/** 宿主提交的回合外变化：明确记录来源及前后状态，不把训练选择或成员退出伪装成核心结算。 */
public record BattleReplayState(String kind, String reason, BattleState before, BattleState after, String previous,
		String hash) {

	/** 状态变化也必须进入同一摘要链；缺失任一快照便不能证明下一回合的输入来源。 */
	public BattleReplayState {
		if (!"STATE".equals(kind) || reason == null || reason.isBlank() || before == null || after == null
				|| previous == null || hash == null)
			throw new IllegalArgumentException("战斗外部状态记录不完整");
	}

	/** 摘要排除自身字段，其他输入全部参与计算。 */
	public BattleReplayState unhashed() {
		return new BattleReplayState(kind, reason, before, after, previous, "");
	}
}
