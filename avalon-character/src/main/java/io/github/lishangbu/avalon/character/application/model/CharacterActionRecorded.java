package io.github.lishangbu.avalon.character.application.model;

/**
 * 已在角色锁保护下发生的真实行为。永久事实只存首次，但每次新行为都发布事件；命令回执重放不能再次发布。 监听器必须同步加入原事务，使行为、任务目标与奖励一起提交或回滚。
 */
public record CharacterActionRecorded(long characterId, String code) {
}
