package io.github.lishangbu.avalon.combat;

import java.util.List;
import java.util.HashSet;
import java.util.Comparator;

/** 不可变阶段扩展目录；所有钩子共用事件和递归预算，空目录表示没有额外扩展。 */
public final class CombatPhaseHooks<S, I> {

	/** 已排序目录不会被调用方后续修改。 */
	private final List<CombatPhaseHook<S, I>> hooks;

	/** 代码全局唯一，阶段明确；重复注册拒绝而非覆盖。 */
	public CombatPhaseHooks(List<? extends CombatPhaseHook<S, I>> extensions) {
		var codes = new HashSet<String>();
		for (var hook : extensions) {
			if (hook.kind() == null || hook.kind().isBlank() || hook.phase() == null || !codes.add(hook.kind()))
				throw new IllegalArgumentException("阶段钩子无效或重复：" + hook.kind());
		}
		hooks = extensions.stream()
			.map(hook -> (CombatPhaseHook<S, I>) hook)
			.sorted(Comparator.comparing(CombatPhaseHook::kind))
			.toList();
	}

	/** 核心当前阶段唯一决定执行集合；各钩子先登记触发事件再修改隔离状态。 */
	public void apply(CombatFrame<S, I> frame) {
		for (var hook : hooks) {
			if (hook.phase() == frame.phase())
				frame.trigger(hook.kind(), () -> hook.apply(frame));
		}
	}

}
