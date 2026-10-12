package io.github.lishangbu.avalon.quest.model;

import static io.github.lishangbu.avalon.quest.model.QuestModelValidation.text;

/** 声明式奖励或消耗。执行顺序有意义，宿主必须在同一事务中执行并保存实例与回执。 */
public record QuestEffect(String kind, String reference, long amount) {
	public QuestEffect {
		text(kind, "效果类型");
		if (amount <= 0)
			throw new IllegalArgumentException("效果数量必须为正数");
	}
}
