package io.github.lishangbu.avalon.world.quest;

import io.github.lishangbu.avalon.quest.model.QuestCondition;

/** 即时条件扩展，读取资格不能发奖或修改任务；未知类型、错误引用在加载时拒绝。 */
public interface QuestConditionHandler {

	String kind();

	void validate(QuestCondition condition);

	boolean test(long owner, QuestCondition condition);

}
