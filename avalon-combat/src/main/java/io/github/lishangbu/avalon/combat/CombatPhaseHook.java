package io.github.lishangbu.avalon.combat;

import io.github.lishangbu.avalon.combat.model.CombatPhase;

/** 阶段扩展契约：声明唯一代码及固定阶段，只在核心允许的阶段执行，不能自行推进阶段。 */
public interface CombatPhaseHook<S, I> {

	/** 稳定代码同时决定同阶段钩子顺序，不能依赖容器装配顺序。 */
	String kind();

	/** 扩展所属的固定阶段。 */
	CombatPhase phase();

	/** 仅处理当前隔离帧，所有嵌套反应须继续使用frame.trigger。 */
	void apply(CombatFrame<S, I> frame);

}
