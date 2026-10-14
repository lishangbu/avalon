package io.github.lishangbu.avalon.battle.domain;

import io.github.lishangbu.avalon.combat.model.CombatEvent;
import io.github.lishangbu.avalon.combat.model.CombatResourceChange;
import java.util.List;

/** 整回合隔离结果。宿主必须原子提交state、changes与回执；events用于提交后文本日志。 */
public record BattleRoundResult(BattleState before, BattleState state, BattleRoundInput input,
		List<CombatResourceChange> changes, List<CombatEvent> events) {
	/** 不允许调用方修改资源结果和随机证据；状态是本回合私有副本。 */
	public BattleRoundResult {
		changes = List.copyOf(changes);
		events = List.copyOf(events);
	}
}
