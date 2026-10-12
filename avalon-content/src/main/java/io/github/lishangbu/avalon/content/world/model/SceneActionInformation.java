package io.github.lishangbu.avalon.content.world.model;

import java.util.List;

/** 直接显示在设施或人物页面的资料；读取不生成事实、回执或奖励。 */
public record SceneActionInformation(String id, String location, String label, String description,
		List<String> requires, String closedByFact, String npcId) {
	public SceneActionInformation {
		requires = requires == null ? List.of() : List.copyOf(requires);
	}
}
