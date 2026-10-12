package io.github.lishangbu.avalon.content.battle.model;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 捕捉课程使用固定等级投影和独立药品库存；回复值不影响正式商店物品。 */
public record AcademyTrainingCapture(int level, String species, Map<String, List<Integer>> stats,
		Map<String, Integer> supplies) {
	public AcademyTrainingCapture {
		stats = stats.entrySet()
			.stream()
			.collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
		supplies = Map.copyOf(supplies);
	}
}
