package io.github.lishangbu.avalon.quest;

import io.github.lishangbu.avalon.quest.model.QuestGrant;
import java.util.List;

/**
 * 状态机一次操作的结果。宿主须锁定归属主体，在一个事务中顺序执行grants并保存instance。 执行失败必须整体回滚；提交后重试读取已持久化回执，不再次执行奖励。
 */
public record QuestTransition(QuestInstance instance, List<QuestGrant> grants) {

	public QuestTransition {
		grants = List.copyOf(grants);
	}

}
