package io.github.lishangbu.avalon.content.quest.model;

import java.util.List;
import java.util.Objects;

/** 任务分类与生命周期独立，完整策略必须由配置显式提供。 */
public record QuestRule(String id, String title, QuestType type, List<String> tags, Boolean abandonable,
		QuestPolicy policy) {
	public QuestRule {
		tags = List.copyOf(tags);
		Objects.requireNonNull(policy, "任务生命周期策略不能为空");
		Objects.requireNonNull(abandonable, "任务放弃策略不能为空");
	}
}
