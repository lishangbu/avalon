package io.github.lishangbu.avalon.combat.model;

/** 每回合的触发预算，限制配置或处理器错误造成的无限反应；超限必须回滚整个宿主事务。 */
public record CombatLimits(int maximumEvents, int maximumDepth) {
	/** 预算必须是有限正数，不允许以零或负数关闭保护。 */
	public CombatLimits {
		if (maximumEvents < 1 || maximumDepth < 1)
			throw new IllegalArgumentException("战斗触发预算必须为正数");
	}

	/** 默认预算覆盖现有多人副本的正常回合；宿主可以显式调整但不能取消保护。 */
	public static CombatLimits defaults() {
		return new CombatLimits(10000, 32);
	}
}
