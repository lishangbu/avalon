package io.github.lishangbu.avalon.content.quest.model;

/** npcId为空表示现场接取；无须为环境目标创造虚构人物。 */
public record QuestReception(String location, String npcId) {
	public QuestReception {
		if (location == null || location.isBlank() || npcId != null && npcId.isBlank())
			throw new IllegalArgumentException("接取地点或人物编号为空");
	}
}
