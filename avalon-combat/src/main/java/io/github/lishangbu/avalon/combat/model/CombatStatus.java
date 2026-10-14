package io.github.lishangbu.avalon.combat.model;

import java.util.HashMap;

import java.util.Map;
import java.util.Objects;

/** 统一状态实例：稳定类型、来源、起止回合、强度与明确参数；不为新增状态扩展单位存档字段。 */
public record CombatStatus(String kind, String source, int started, int until, long strength, Map<String, Long> values,
		Map<String, String> references) {
	/** 复制所有参数，允许零期限表示不按回合过期的机制记忆；有效性由状态定义处理器校验。 */
	public CombatStatus {
		if (kind == null || kind.isBlank() || started < 0 || until < 0 || strength < 0)
			throw new IllegalArgumentException("战斗状态参数无效");
		Objects.requireNonNull(source, "状态来源不能为空");
		values = Map.copyOf(values);
		references = Map.copyOf(references);
	}

	/** 更新一项数值时生成新实例，已记录的旧状态不能被后续效果改写。 */
	public CombatStatus with(String name, long value) {
		var updated = new HashMap<>(values);
		updated.put(name, value);
		return new CombatStatus(kind, source, started, until, strength, updated, references);
	}
}
