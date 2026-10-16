package io.github.lishangbu.avalon.character.application.model;

import java.time.Instant;

/**
 * 持角色锁后真实发生的行为，永久事实只保存首次，但每次新行为均发布新事件。
 * owner只在服务端生成供宿主查资产；eventId保证观察器重放不重复计数，amount支持批量获得。 监听器同步加入原事务，行为、进度和奖励一起提交或回滚。
 */
public record CharacterActionRecorded(long characterId, String code, long owner, String eventId, long amount,
		Instant at) {
}
