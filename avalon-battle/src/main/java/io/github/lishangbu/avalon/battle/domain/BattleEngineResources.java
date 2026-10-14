package io.github.lishangbu.avalon.battle.domain;

/** 宿主资源端口；行动者必须显式传入，单人和多人使用相同归属协议。核心执行只使用快照账本实现。 */
public interface BattleEngineResources {

	/** 读取指定行动者的库存，不共享其他成员库存。 */
	int count(BattleStateUnit actor, String code);

	/** 宿主持锁校验目标等级和捕获容量，快照将结果固定在本回合。 */
	boolean room(BattleStateUnit target);

	/** 应用层提交一份已通过校验的物品变更，失败必须使外层事务回滚。 */
	void take(BattleStateUnit actor, String code);

	/** 应用层提交已确认的捕获结果，不在规则计算过程中创建真实个体。 */
	void capture(BattleStateUnit target);

}
