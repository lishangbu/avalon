package io.github.lishangbu.avalon.content.quest.model;

import io.github.lishangbu.avalon.quest.model.*;
import java.util.List;
import java.util.Objects;

/** 唯一任务步骤格式：显式目标、条件及效果，接取补给与完成奖励分别结算。 */
public record QuestContentStep(String id, String quest, String title, String location, String label, String dialogue,
		List<String> requires, boolean completes, List<QuestEffect> effects, List<QuestObjective> objectives,
		List<QuestCondition> conditions, QuestDelivery delivery, QuestBranch branch, QuestTimeout timeout, String npcId,
		boolean environmental, List<QuestEffect> acceptEffects, QuestReception acceptance) {
	public QuestContentStep {
		requires = List.copyOf(requires);
		effects = List.copyOf(effects);
		objectives = List.copyOf(objectives);
		conditions = List.copyOf(conditions);
		acceptEffects = List.copyOf(acceptEffects);
		Objects.requireNonNull(delivery, "任务交付方式不能为空");
		Objects.requireNonNull(acceptance, "任务接取地点不能为空");
	}
}
