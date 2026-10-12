package io.github.lishangbu.avalon.world.application.model;

import java.util.Map;

/** 每角色一块农田，两地入口读取同一状态；空格没有作物记录，种子绑定随收获传播。 */
public record FarmState(int plots, Map<Integer, FarmStateCrop> crops) {

	public FarmState {
		crops = Map.copyOf(crops);
		if (plots < 6 || plots > 24 || crops.keySet().stream().anyMatch(index -> index < 0 || index >= plots))
			throw new IllegalArgumentException("农田格数或种植位置无效");
	}

}
