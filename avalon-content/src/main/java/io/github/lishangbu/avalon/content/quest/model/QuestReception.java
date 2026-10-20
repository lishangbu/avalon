package io.github.lishangbu.avalon.content.quest.model;

/** npcId为空表示现场接取；无须为环境目标创造虚构人物。 */
public record QuestReception(String location, String npcId, boolean explicit) {
	/** 默认沿用整章参与状态；显式标记的步骤即使章节已接取，也必须在指定入口单独登记。 */
	public QuestReception(String location, String npcId) {
		this(location, npcId, false);
	}

	public QuestReception {
		if (location == null || location.isBlank() || npcId != null && npcId.isBlank())
			throw new IllegalArgumentException("接取地点或人物编号为空");
	}
}
