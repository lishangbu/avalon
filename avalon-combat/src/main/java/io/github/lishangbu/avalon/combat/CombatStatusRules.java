package io.github.lishangbu.avalon.combat;

import io.github.lishangbu.avalon.combat.model.*;
import java.util.Map;

/** 通用状态的安装、叠加、到期与移除算法；具体跳点与免疫属于可注册游戏规则。 */
public final class CombatStatusRules {

	/** 同类同来源键由调用方明确选择，印记可按来源分别保存；无隐式跨阵营共享。 */
	public void install(Map<String, CombatStatus> states, String key, CombatStatus next, CombatStatusStacking stacking,
			long maximum) {
		if (key == null || key.isBlank() || maximum < 0 || next.strength() > maximum)
			throw new IllegalArgumentException("状态键或叠加上限无效");
		var prior = states.get(key);
		if (prior != null && !prior.kind().equals(next.kind()))
			throw new IllegalArgumentException("同一状态键不能改变类型");
		if (prior == null || stacking == CombatStatusStacking.REPLACE) {
			states.put(key, next);
			return;
		}
		if (stacking == CombatStatusStacking.STRONGEST) {
			if (next.strength() >= prior.strength())
				states.put(key, next);
			return;
		}
		long combined = Math.min(maximum, Math.addExact(prior.strength(), next.strength()));
		states.put(key, new CombatStatus(next.kind(), next.source(), next.started(), next.until(), combined,
				next.values(), next.references()));
	}

	/** 到期当轮仍生效，因此只移除期限严格小于当前回合的状态；零期限由机制显式移除。 */
	public void expire(Map<String, CombatStatus> states, int round) {
		states.values().removeIf(value -> value.until() > 0 && value.until() < round);
	}

	/** 死亡、退出或驱散由规则明确选择键集合，不把所有机制记忆无条件删除。 */
	public void remove(Map<String, CombatStatus> states, String key) {
		states.remove(key);
	}

}
