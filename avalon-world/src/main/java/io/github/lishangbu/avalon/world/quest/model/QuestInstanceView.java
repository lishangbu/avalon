package io.github.lishangbu.avalon.world.quest.model;

import io.github.lishangbu.avalon.quest.model.QuestStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** 玩家视图只含当前可理解的进度，过滤内部事件去重键与资产结算账本。 */
public record QuestInstanceView(String id, String quest, String period, int round, QuestStatus status, Instant deadline,
		int ringSize, List<QuestGoalView> goals, List<QuestBranchView> branches, Map<String, String> choices,
		boolean timed) {
}
