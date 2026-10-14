package io.github.lishangbu.avalon.combat.model;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.Collections;

/** 机器可核对的事件；文字战报由宿主生成，事件不包含账户凭据或数据库实体。 */
public record CombatEvent(long sequence, CombatPhase phase, String kind, Map<String, String> values) {
	/** 防止处理器事后修改历史事件；按键排序使文本序列化与核对稳定。 */
	public CombatEvent {
		if (sequence < 0 || kind == null || kind.isBlank())
			throw new IllegalArgumentException("战斗事件身份无效");
		Objects.requireNonNull(phase, "战斗事件阶段不能为空");
		values = Collections.unmodifiableMap(new TreeMap<>(values));
	}
}
