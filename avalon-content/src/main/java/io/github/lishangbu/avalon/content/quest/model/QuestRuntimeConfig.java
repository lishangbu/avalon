package io.github.lishangbu.avalon.content.quest.model;

import io.github.lishangbu.avalon.quest.model.QuestBranch;
import io.github.lishangbu.avalon.quest.model.QuestCondition;
import io.github.lishangbu.avalon.quest.model.QuestDelivery;
import io.github.lishangbu.avalon.quest.model.QuestEffect;
import io.github.lishangbu.avalon.quest.model.QuestObjective;
import io.github.lishangbu.avalon.quest.model.QuestTimeout;
import java.util.List;

/**
 * 新能力使用通用目标、条件与效果。旧facts和effects仍可配置，并与这里的声明合并； 新增扩展类型只需注册宿主处理器，不需要扩展旧Kind枚举或修改引擎。
 */
public record QuestRuntimeConfig(List<QuestObjective> objectives, List<QuestCondition> conditions,
		List<QuestEffect> effects, QuestDelivery delivery, QuestBranch branch, QuestTimeout timeout) {
	public QuestRuntimeConfig {
		objectives = objectives == null ? List.of() : List.copyOf(objectives);
		conditions = conditions == null ? List.of() : List.copyOf(conditions);
		effects = effects == null ? List.of() : List.copyOf(effects);
		delivery = delivery == null ? QuestDelivery.MANUAL : delivery;
	}
}
