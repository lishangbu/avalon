package io.github.lishangbu.avalon.combat;

import io.github.lishangbu.avalon.combat.model.*;
import java.util.Map;
import java.util.Objects;

/** JDK战斗核心：控制固定阶段、隔离副本和触发预算，不包含任何Avalon玩法判断。 */
public final class CombatRoundEngine {

	/** 从已接受输入执行一整回合。异常不产生可提交结果，宿主不能提交半轮状态或资源变更。 */
	public <S, I> CombatTransition<S> resolve(S original, I input, CombatRoundRules<S, I> rules, CombatLimits limits) {
		Objects.requireNonNull(original, "战斗状态不能为空");
		Objects.requireNonNull(input, "战斗输入不能为空");
		Objects.requireNonNull(rules, "战斗规则不能为空");
		Objects.requireNonNull(limits, "战斗预算不能为空");
		if (rules.terminal(original))
			throw new IllegalStateException("已经结束的战斗不能执行回合");
		if (rules.round(original) < 1)
			throw new IllegalArgumentException("战斗回合编号无效");
		S state = Objects.requireNonNull(rules.copy(original), "规则必须复制战斗状态");
		if (state == original)
			throw new IllegalStateException("规则不能复用输入状态作为执行副本");
		var frame = new CombatFrame<>(state, input, limits);
		for (var phase : CombatPhase.values()) {
			frame.phase(phase);
			if (rules.terminal(state) && phase != CombatPhase.CLOSE)
				continue;
			frame.event("PHASE", Map.of("name", phase.name()));
			rules.apply(phase, frame);
		}
		return new CombatTransition<>(state, frame.events());
	}

}
