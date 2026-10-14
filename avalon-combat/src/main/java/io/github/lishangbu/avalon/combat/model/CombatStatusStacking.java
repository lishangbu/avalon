package io.github.lishangbu.avalon.combat.model;

/** 状态叠加策略由配置明确选择，不根据处理器注册顺序或来源编号推断。 */
public enum CombatStatusStacking {

	/** 新状态完整覆盖旧状态，持续恢复等快照不得相加。 */
	REPLACE,
	/** 只接收不弱于当前状态的新实例，刷新期限也必须满足强度条件。 */
	STRONGEST,
	/** 强度累加并受定义上限约束，期限取新实例。 */
	ADD

}
