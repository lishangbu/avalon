package io.github.lishangbu.avalon.world.quest.model;

/** 参数仅选择操作和现有实例；客户端无权提交目标数量、行为事实或奖励定义。 */
public record QuestInstanceRequest(String requestId, String instance, int round, QuestOperation operation, String step,
		String group, String option,
		java.util.List<io.github.lishangbu.avalon.creature.application.model.CreatureView> selectedPets) {
	public QuestInstanceRequest(String requestId, String instance, int round, QuestOperation operation, String step,
			String group, String option) {
		this(requestId, instance, round, operation, step, group, option, java.util.List.of());
	}

	public QuestInstanceRequest {
		selectedPets = selectedPets == null ? java.util.List.of() : java.util.List.copyOf(selectedPets);
	}
}
