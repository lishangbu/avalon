package io.github.lishangbu.avalon.content.quest.model;

import java.util.List;

/** 重复策略与放弃资格独立配置；标签仅用于显示和筛选，不授予执行资格。 */
public record QuestRule(String id, String title, QuestType type, List<String> tags, QuestRepeatPolicy repeatPolicy,
		Boolean abandonable, QuestPolicy policy) {
	/** 旧内容构造入口沿用一次性默认策略，新增规则可显式指定周期、限时与次数。 */
	public QuestRule(String id, String title, QuestType type, List<String> tags, QuestRepeatPolicy repeatPolicy,
			Boolean abandonable) {
		this(id, title, type, tags, repeatPolicy, abandonable, null);
	}

	public QuestRule {
		tags = List.copyOf(tags);
		policy = policy == null ? new QuestPolicy(null, 1, 10, null, false, List.of()) : policy;
		// 主线默认不可放弃，支线默认可放弃；显式配置仍须经过主线约束校验。
		abandonable = abandonable == null ? type == QuestType.SIDE : abandonable;
	}
}
