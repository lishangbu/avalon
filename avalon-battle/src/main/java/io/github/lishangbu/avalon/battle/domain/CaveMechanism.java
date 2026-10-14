package io.github.lishangbu.avalon.battle.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** 獠牙兽王策略私有状态视图；公共战局仅保存mechanisms，不再暴露首领专属访问器。 */
public final class CaveMechanism {

	/** 视图仅在隔离回合或持锁宿主中使用，不缓存跨战局状态。 */
	private final BattleState state;

	/** 将策略视图绑定到明确战局，所有读写仍作用于统一机制集合。 */
	public CaveMechanism(BattleState state) {
		this.state = Objects.requireNonNull(state);
	}

	/** 仅供当前游戏CAVE策略的类型化读取，保存格式始终是mechanisms而非旧字段。 */
	private String mechanism(String name, String fallback) {
		return state.getMechanisms().getOrDefault("CAVE", Map.of()).getOrDefault(name, fallback);
	}

	/** 更新私有机制数据，空引用明确移除，不能在重连时凭空重新生成目标。 */
	private void updateMechanism(String name, String value) {
		var data = state.getMechanisms().computeIfAbsent("CAVE", key -> new HashMap<>());
		if (value == null)
			data.remove(name);
		else
			data.put(name, value);
	}

	/** CAVE策略的phase，不参与旧JSON字段解析。 */
	public int getBossPhase() {
		return Integer.parseInt(mechanism("phase", "1"));
	}

	/** 仅修改CAVE策略私有状态，不影响其他AI或首领。 */
	public void setBossPhase(int value) {
		updateMechanism("phase", Integer.toString(value));
	}

	/** CAVE策略的cursor，不参与旧JSON字段解析。 */
	public int getBossCursor() {
		return Integer.parseInt(mechanism("cursor", "0"));
	}

	/** 仅修改CAVE策略私有状态，不影响其他AI或首领。 */
	public void setBossCursor(int value) {
		updateMechanism("cursor", Integer.toString(value));
	}

	/** CAVE策略的targetCursor，不参与旧JSON字段解析。 */
	public int getBasicTargetCursor() {
		return Integer.parseInt(mechanism("targetCursor", "0"));
	}

	/** 仅修改CAVE策略私有状态，不影响其他AI或首领。 */
	public void setBasicTargetCursor(int value) {
		updateMechanism("targetCursor", Integer.toString(value));
	}

	/** CAVE策略的heavyExecuted，不参与旧JSON字段解析。 */
	public boolean isHeavyExecuted() {
		return !"0".equals(mechanism("heavyExecuted", "0"));
	}

	/** 仅修改CAVE策略私有状态，不影响其他AI或首领。 */
	public void setHeavyExecuted(boolean value) {
		updateMechanism("heavyExecuted", value ? "1" : "0");
	}

	/** CAVE策略的markedTarget，不参与旧JSON字段解析。 */
	public String getMarkedTarget() {
		return mechanism("markedTarget", null);
	}

	/** 仅修改CAVE策略私有状态，不影响其他AI或首领。 */
	public void setMarkedTarget(String value) {
		updateMechanism("markedTarget", value);
	}

}
