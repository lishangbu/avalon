package io.github.lishangbu.avalon.content.world.model;

import java.util.List;

/** 优先级按配置顺序递减，先选已解锁的较后章节城镇。 */
public record WorldSafeReturn(String location, String requires, List<String> fromLocations) {
	public WorldSafeReturn {
		fromLocations = fromLocations == null ? List.of() : List.copyOf(fromLocations);
	}
}
