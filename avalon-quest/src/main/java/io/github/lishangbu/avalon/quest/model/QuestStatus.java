package io.github.lishangbu.avalon.quest.model;

/** READY表示整项（或当前委托）目标已经达成但仍待交付，跨周期保留领取资格。 */
public enum QuestStatus {

	ACTIVE, PAUSED, READY, COMPLETED, FAILED, EXPIRED, ABANDONED

}
