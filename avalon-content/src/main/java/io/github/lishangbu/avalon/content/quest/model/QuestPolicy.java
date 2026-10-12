package io.github.lishangbu.avalon.content.quest.model;

import io.github.lishangbu.avalon.quest.model.QuestEffect;
import io.github.lishangbu.avalon.quest.model.QuestTimeout;
import java.time.ZoneId;
import java.util.List;

/**
 * 生命周期配置与任务类型独立。每日/每周限制完成次数，委托环限制每日开启次数； 不保存定义版本，配置变更约束由部署时的结构校验执行。
 */
public record QuestPolicy(String timezone, int limit, int ringSize, QuestTimeout timeout, boolean retryable,
		List<QuestEffect> ringRewards) {
	public QuestPolicy {
		timezone = timezone == null ? "Asia/Hong_Kong" : timezone;
		ZoneId.of(timezone);
		ringRewards = ringRewards == null ? List.of() : List.copyOf(ringRewards);
		if (limit <= 0 || ringSize <= 0)
			throw new IllegalArgumentException("任务次数或环长度必须为正数");
	}
}
