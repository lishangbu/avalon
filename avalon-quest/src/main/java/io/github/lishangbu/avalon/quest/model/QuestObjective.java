package io.github.lishangbu.avalon.quest.model;

import static io.github.lishangbu.avalon.quest.model.QuestModelValidation.text;

/**
 * 目标编号在所属步骤内唯一。code由宿主命名，例如KILL:WOLF或ITEM:POTION； EVENT每次真实行为可推进多个实例，但相同事件编号在同一实例只计算一次。
 */
public record QuestObjective(String id, QuestMeasure measure, String code, long amount) {
	public QuestObjective {
		text(id, "目标编号");
		text(code, "目标代码");
		if (measure == null || amount <= 0)
			throw new IllegalArgumentException("目标模式或数量无效");
	}
}
