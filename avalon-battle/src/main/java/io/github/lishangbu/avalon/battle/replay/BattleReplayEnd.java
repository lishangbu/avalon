package io.github.lishangbu.avalon.battle.replay;

import java.time.Instant;

/** 已结束标记和最终链摘要；清理只依据明确终局记录，活动文件或残缺尾部永不按年龄删除。 */
public record BattleReplayEnd(String kind, String status, Instant ended, String previous) {

	/** 终局不允许ACTIVE，结束时刻与链摘要必须存在。 */
	public BattleReplayEnd {
		if (!"END".equals(kind) || status == null || status.equals("ACTIVE") || ended == null || previous == null)
			throw new IllegalArgumentException("战斗结束记录不完整");
	}
}
