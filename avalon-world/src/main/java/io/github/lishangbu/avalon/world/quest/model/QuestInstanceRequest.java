package io.github.lishangbu.avalon.world.quest.model;

import io.github.lishangbu.avalon.creature.application.model.CreatureView;
import java.util.List;

/** 参数仅选择操作和现有实例；客户端无权提交目标数量、行为事实或奖励定义。 */
public record QuestInstanceRequest(String requestId, String instance, int round, QuestOperation operation, String step,
		String group, String option, List<CreatureView> selectedPets) {
	public QuestInstanceRequest(String requestId, String instance, int round, QuestOperation operation, String step,
			String group, String option) {
		this(requestId, instance, round, operation, step, group, option, List.of());
	}

	public QuestInstanceRequest {
		selectedPets = selectedPets == null ? List.of() : List.copyOf(selectedPets);
	}
}
