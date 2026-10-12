package io.github.lishangbu.avalon.world.quest.model;

import io.github.lishangbu.avalon.quest.QuestInstance;

import java.util.List;

/** 实例命令的完整响应，含首次交付对白；内部非交付操作使用通用状态说明。 */
public record QuestApplied(QuestInstance instance, List<String> rewards, String dialogue) {
	public QuestApplied(QuestInstance instance, List<String> rewards) {
		this(instance, rewards, "任务状态已更新");
	}

	/** 响应不可变，避免上层通知或回执编排修改已经结算的奖励说明。 */
	public QuestApplied {
		rewards = List.copyOf(rewards);
	}
}
