package io.github.lishangbu.avalon.quest;

import io.github.lishangbu.avalon.quest.model.QuestCondition;
import io.github.lishangbu.avalon.quest.model.QuestMeasure;

/**
 * 核心与宿主的查询接口。宿主提供持有量、永久历史及即时条件；核心自行管理接取后的事件计数。
 * 查询不得接取任务或发奖。奖励由返回的Transition统一交给宿主事务执行，避免半完成状态。
 */
public interface QuestHost {

	/** HOLDING读取当前数量，HISTORY读取永久记录；EVENT由引擎累计，不调用本方法。 */
	long value(QuestMeasure measure, String code);

	/** 条件类型必须预先注册；未知条件应报配置错误，不能默认满足。 */
	boolean test(QuestCondition condition);

}
