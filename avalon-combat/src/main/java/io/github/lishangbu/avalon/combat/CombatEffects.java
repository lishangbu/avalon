package io.github.lishangbu.avalon.combat;

import java.util.*;
import java.util.function.Function;

/** 按稳定类型注册效果，并严格按配置顺序执行；不含未知类型的默认伤害或降级实现。 */
public final class CombatEffects<E, C> {

	private final Map<String, CombatEffectHandler<E, C>> handlers;

	private final Function<E, String> kind;

	/** 完成注册后目录不可变，重复或空类型拒绝，避免Spring装配顺序决定游戏行为。 */
	public CombatEffects(List<CombatEffectHandler<E, C>> extensions, Function<E, String> kind) {
		this.kind = Objects.requireNonNull(kind);
		var registered = new HashMap<String, CombatEffectHandler<E, C>>();
		for (var handler : extensions) {
			String code = handler.kind();
			if (code == null || code.isBlank() || registered.putIfAbsent(code, handler) != null)
				throw new IllegalArgumentException("战斗效果重复或无效：" + code);
		}
		handlers = Map.copyOf(registered);
	}

	/** 每个效果都经同一个处理器校验，调用方应在内容发布时对全部技能执行此检查。 */
	public void validate(List<E> effects) {
		effects.forEach(effect -> required(effect).validate(effect));
	}

	/** 有序组合不重复支付技能费用；调用方在组合前统一支付，在整体失败时回滚。 */
	public void apply(List<E> effects, C context, CombatFrame<?, ?> frame) {
		for (var effect : effects)
			frame.trigger(kind.apply(effect), () -> required(effect).apply(effect, context));
	}

	/** 未注册效果直接失败，绝不猜测成普攻或跳过。 */
	private CombatEffectHandler<E, C> required(E effect) {
		var handler = handlers.get(kind.apply(effect));
		if (handler == null)
			throw new IllegalArgumentException("未注册战斗效果：" + kind.apply(effect));
		return handler;
	}

}
