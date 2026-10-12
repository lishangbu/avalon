package io.github.lishangbu.avalon.content.quest.model;

import java.util.List;

/** 任务规则与标签配置根对象；构造时保存不可变列表。 */
public record QuestRulesDefinition(List<QuestTag> tags, List<QuestRule> quests) {
	public QuestRulesDefinition {
		tags = List.copyOf(tags);
		quests = List.copyOf(quests);
	}
}
