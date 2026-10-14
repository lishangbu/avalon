package io.github.lishangbu.avalon.battle.domain;

import java.util.List;
import java.util.HashSet;
import org.springframework.stereotype.Component;

/** 不可变敌方策略目录；新增首领可注册独立策略，无须增加回合引擎中的玩法分支。 */
@Component
public class BattleEnemyPolicies {

	/** 已装配策略不保留调用方可变列表。 */
	private final List<BattleEnemyPolicy> policies;

	/** 重复代码或空目录立即拒绝启动，不能用排序隐式覆盖另一份规则。 */
	public BattleEnemyPolicies(List<BattleEnemyPolicy> policies) {
		var kinds = new HashSet<String>();
		for (var policy : policies) {
			if (policy.kind() == null || policy.kind().isBlank() || !kinds.add(policy.kind()))
				throw new IllegalArgumentException("敌方策略代码无效或重复：" + policy.kind());
		}
		if (policies.isEmpty())
			throw new IllegalArgumentException("敌方策略目录不能为空");
		this.policies = List.copyOf(policies);
	}

	/** 要求匹配明确且唯一；错误配置不会默默降级成野战。 */
	public BattleEnemyPolicy require(BattleState state) {
		var matches = policies.stream().filter(policy -> policy.supports(state)).toList();
		if (matches.size() != 1)
			throw new IllegalArgumentException("战局必须且只能匹配一个敌方策略，实际为：" + matches.size());
		return matches.getFirst();
	}

}
