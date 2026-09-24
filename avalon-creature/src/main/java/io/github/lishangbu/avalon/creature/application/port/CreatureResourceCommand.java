package io.github.lishangbu.avalon.creature.application.port;

/** 生活生产在外层事务内消费伙伴资源；实现由持有真实生命魔法的战斗模块提供。 */
public interface CreatureResourceCommand {

	/** 固定消耗，不允许致死；不足返回false且不改变资源。调用方须将成功产出纳入同一事务。 */
	boolean spendForProduction(long owner, long creature, int health, int mana);

	/** 内部生产事务已验证并扣除药品后恢复资源，不提供直接HTTP入口。 */
	void restoreForProduction(long owner, long creature, int health, int mana);

}
