package io.github.lishangbu.avalon.battle.replay;

import io.github.lishangbu.avalon.battle.domain.BattleState;
import java.time.Instant;
import java.util.Map;

/** 每场文本记录首行包含初始战局和完整内容，明确区分个人与多人编号空间。 */
public record BattleReplayHeader(String kind, String battle, String mode, Instant created, String implementation,
		Map<String, String> rules, BattleState initial) {

	/** 不允许缺省实现或规则；任何缺失都意味着记录无法用于精确重放。 */
	public BattleReplayHeader {
		if (!"HEADER".equals(kind) || battle == null || battle.isBlank() || mode == null || created == null
				|| implementation == null || implementation.isBlank() || initial == null)
			throw new IllegalArgumentException("战斗记录头不完整");
		rules = Map.copyOf(rules);
		if (rules.isEmpty())
			throw new IllegalArgumentException("战斗记录必须包含规则快照");
	}
}
