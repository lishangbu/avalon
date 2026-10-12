package io.github.lishangbu.avalon.quest.model;

import java.util.List;
import static io.github.lishangbu.avalon.quest.model.QuestModelValidation.text;
import static io.github.lishangbu.avalon.quest.model.QuestModelValidation.unique;

/**
 * 步骤依赖组成无环图。公共汇合步骤可以同时依赖多个互斥路线的末端： 选择后，未选路线视为跳过，只有所选路线的依赖需要实际完成。
 */
public record QuestStep(String id, List<String> requires, List<QuestObjective> objectives,
		List<QuestCondition> conditions, List<QuestEffect> effects, QuestDelivery delivery, QuestBranch branch,
		QuestTimeout timeout, List<QuestCondition> activation) {
	/** 普通任务无需额外开放条件；宿主跨任务前置可用activation声明而不污染交付条件。 */
	public QuestStep(String id, List<String> requires, List<QuestObjective> objectives, List<QuestCondition> conditions,
			List<QuestEffect> effects, QuestDelivery delivery, QuestBranch branch, QuestTimeout timeout) {
		this(id, requires, objectives, conditions, effects, delivery, branch, timeout, List.of());
	}

	public QuestStep {
		text(id, "步骤编号");
		requires = requires == null ? List.of() : List.copyOf(requires);
		objectives = objectives == null ? List.of() : List.copyOf(objectives);
		conditions = conditions == null ? List.of() : List.copyOf(conditions);
		activation = activation == null ? List.of() : List.copyOf(activation);
		effects = effects == null ? List.of() : List.copyOf(effects);
		delivery = delivery == null ? QuestDelivery.MANUAL : delivery;
		unique(requires, "步骤前置");
		unique(objectives.stream().map(QuestObjective::id).toList(), "目标编号");
	}
}
