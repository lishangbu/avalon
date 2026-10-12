package io.github.lishangbu.avalon.quest.model;

import static io.github.lishangbu.avalon.quest.model.QuestModelValidation.text;

/** 宿主注册的即时条件；reference为空可表示不带引用的条件，amount含义由处理器定义。 */
public record QuestCondition(String kind, String reference, long amount) {
	public QuestCondition {
		text(kind, "条件类型");
	}
}
