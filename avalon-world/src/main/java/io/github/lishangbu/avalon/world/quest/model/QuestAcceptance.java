package io.github.lishangbu.avalon.world.quest.model;

import io.github.lishangbu.avalon.quest.QuestInstance;

import java.util.List;

/** 接取返回实际实例及新建标记，补给写入口据此区分恢复与全新尝试。 */
public record QuestAcceptance(QuestInstance instance, boolean fresh, List<String> rewards) {
	public QuestAcceptance {
		rewards = List.copyOf(rewards);
	}
}
