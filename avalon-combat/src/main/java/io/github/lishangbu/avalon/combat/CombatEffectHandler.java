package io.github.lishangbu.avalon.combat;

/** 可注册效果契约，E为类型化配置，C为隔离执行上下文；新增能力不要求修改核心。 */
public interface CombatEffectHandler<E, C> {

	/** 稳定类型代码，在同一注册器中必须唯一。 */
	String kind();

	/** 内容加载时校验参数与引用；错误应拒绝部署，不能等玩家施放后才发现。 */
	void validate(E effect);

	/** 执行纯状态变更；不得调用真实资产服务或修改处理器共享字段。 */
	void apply(E effect, C context);

}
