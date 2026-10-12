package io.github.lishangbu.avalon.content.quest.model;

import java.util.Map;

/** 按稳定步骤编号索引的阶段对白；构造时复制为不可变映射。 */
public record QuestDialoguesDefinition(Map<String, QuestDialogue> steps) {
	public QuestDialoguesDefinition {
		steps = Map.copyOf(steps);
	}
}
