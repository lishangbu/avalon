package io.github.lishangbu.avalon.content.quest.model;

import java.util.List;

/** 步骤配置根对象；复制列表防止加载后的内容被调用者修改。 */
public record QuestContentDefinition(List<QuestContentStep> steps) {
	public QuestContentDefinition {
		steps = List.copyOf(steps);
	}
}
