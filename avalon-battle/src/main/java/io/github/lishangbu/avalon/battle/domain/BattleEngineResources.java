package io.github.lishangbu.avalon.battle.domain;

/** 战斗引擎的资源接口。单人适配器按当前角色操作，多人适配器按行动单位归属定位资源；库存扣除和捕获由宿主持锁事务执行。 */
public interface BattleEngineResources {

	int count(String code);

	/** 多人资源适配器按行动人物定位入本物资；个人战斗沿用原适配器。 */
	default int count(BattleStateUnit actor, String code) {
		return count(code);
	}

	boolean room(BattleStateUnit target);

	void take(String code);

	default void take(BattleStateUnit actor, String code) {
		take(code);
	}

	void capture(BattleStateUnit target);

}
