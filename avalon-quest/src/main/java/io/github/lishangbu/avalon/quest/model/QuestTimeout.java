package io.github.lishangbu.avalon.quest.model;

import java.time.Instant;

/**
 * relativeSeconds从实例接取或步骤开放时计时，deadline是绝对截止时刻，二者只能配置其一。
 * 同时配置实例计时和步骤计时是允许的，最先到期的约束决定失败；下线不会暂停时钟。
 */
public record QuestTimeout(Long relativeSeconds, Instant deadline) {
	public QuestTimeout {
		if ((relativeSeconds == null) == (deadline == null) || relativeSeconds != null && relativeSeconds <= 0)
			throw new IllegalArgumentException("限时必须且只能设置正秒数或固定截止时间");
	}

	/** 截止时刻本身已超时；使用Instant避免夏令时和本地时区导致时长漂移。 */
	public Instant at(Instant start) {
		return deadline == null ? start.plusSeconds(relativeSeconds) : deadline;
	}
}
