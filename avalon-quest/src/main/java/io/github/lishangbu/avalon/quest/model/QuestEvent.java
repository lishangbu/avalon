package io.github.lishangbu.avalon.quest.model;

import java.time.Instant;
import static io.github.lishangbu.avalon.quest.model.QuestStatus.*;

/** 外部行为可由击杀、拾取、访问等真实命令生成；编号不由玩家提供。 */
public record QuestEvent(String id, String code, long amount, Instant at) {
	public QuestEvent {
		if (id == null || id.isBlank() || code == null || code.isBlank() || amount <= 0 || at == null)
			throw new IllegalArgumentException("任务行为事件不完整");
	}
}
