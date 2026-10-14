package io.github.lishangbu.avalon.combat.model;

import java.util.List;
import java.util.Objects;

/** 完整回合的纯输出。宿主只有将状态与资源变更同事务保存后，才能向客户端宣布提交成功。 */
public record CombatTransition<S>(S state, List<CombatEvent> events) {
	/** 事件集合不可变；state 是宿主通过 copy 创建的本次隔离副本，不能复用输入实例。 */
	public CombatTransition {
		Objects.requireNonNull(state, "战斗结果不能为空");
		events = List.copyOf(events);
	}
}
