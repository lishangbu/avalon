package io.github.lishangbu.avalon.battle.replay;

import io.github.lishangbu.avalon.battle.domain.BattleState;
import io.github.lishangbu.avalon.battle.domain.BattleRoundResult;

/** 一行完整回合证据；before包含成员退出等宿主事件后的正式输入，result固定纯规则计算输出。 */
public record BattleReplayRound(String kind, int round, String request, BattleState before, BattleRoundResult result,
		String previous, String hash) {

	/** 回合、请求和链摘要均必须存在，不能把半次技能记录当作完整回合。 */
	public BattleReplayRound {
		if (!"ROUND".equals(kind) || round < 1 || request == null || request.isBlank() || before == null
				|| result == null || previous == null || hash == null)
			throw new IllegalArgumentException("战斗回合记录不完整");
	}

	/** 摘要不包含自身hash字段；固定所有其他内容，防止删除或替换中间回合。 */
	public BattleReplayRound unhashed() {
		return new BattleReplayRound(kind, round, request, before, result, previous, "");
	}
}
