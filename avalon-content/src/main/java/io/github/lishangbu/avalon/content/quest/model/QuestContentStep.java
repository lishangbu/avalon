package io.github.lishangbu.avalon.content.quest.model;

import java.util.List;

/** 任务仅引用稳定人物编号；环境步骤不设置人物，奖励和接取补给分别配置。 */
public record QuestContentStep(String id, String quest, String title, String location, String label, String dialogue,
		List<String> requires, boolean completes, List<QuestContentEffect> effects, QuestPlacement placement,
		List<String> facts, int minimumActive, boolean onArrival, String npcId, boolean environmental,
		List<QuestContentEffect> acceptEffects, QuestReception acceptance, QuestRuntimeConfig runtime) {
	/** 保留旧目录及已有调用的构造方式，runtime缺省时由游戏适配器映射现有事实和效果。 */
	public QuestContentStep(String id, String quest, String title, String location, String label, String dialogue,
			List<String> requires, boolean completes, List<QuestContentEffect> effects, QuestPlacement placement,
			List<String> facts, int minimumActive, boolean onArrival, String npcId, boolean environmental,
			List<QuestContentEffect> acceptEffects, QuestReception acceptance) {
		this(id, quest, title, location, label, dialogue, requires, completes, effects, placement, facts, minimumActive,
				onArrival, npcId, environmental, acceptEffects, acceptance, null);
	}

	public QuestContentStep {
		// 多数委托在同一处接取与回报，只有不同地点的委托需要显式覆盖，不能让页面猜测。
		acceptance = acceptance == null ? new QuestReception(location, npcId) : acceptance;
		requires = requires == null ? List.of() : List.copyOf(requires);
		effects = effects == null ? List.of() : List.copyOf(effects);
		acceptEffects = acceptEffects == null ? List.of() : List.copyOf(acceptEffects);
		facts = facts == null ? List.of() : List.copyOf(facts);
		if (facts.stream().anyMatch(code -> code == null || code.isBlank()))
			throw new IllegalArgumentException("任务事实条件为空");
		if (minimumActive < 0 || minimumActive > 2)
			throw new IllegalArgumentException("任务出战数量条件无效");
	}
}
