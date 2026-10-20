package io.github.lishangbu.avalon.world.domain;

import java.time.Duration;
import java.time.Instant;

/** 在线采集计时。时间由服务端传入；断线窗口不计时，已确认的本轮进度仍保留。 */
public final class GatheringRules {

	public static final long HEARTBEAT_TIMEOUT_MILLIS = 2500;

	public static final long MAX_CREDIT_MILLIS = 1000;

	private GatheringRules() {
	}

	public static GatheringRulesCost cost(int tier) {
		if (tier < 0 || tier > 4)
			throw new IllegalArgumentException("采集档位无效");
		return new GatheringRulesCost(5 * (tier + 1), 2 * (tier + 1));
	}

	/** 采集熟练度1～10级决定周期，伙伴等级只影响可用资源，不替代专业成长。 */
	public static long roundMillisForSkill(int skillLevel) {
		if (skillLevel < 1 || skillLevel > 10)
			throw new IllegalArgumentException("采集等级无效");
		return Math.max(6000, 10000 - (skillLevel - 1) * 500L);
	}

	/** 每个心跳至多推进一秒；超时后的首个心跳仅重新建立在线基点，不补算断线窗口。 */
	public static GatheringRulesProgress heartbeat(long confirmedMillis, long roundMillis, Instant lastSeen,
			Instant now, boolean foreground) {
		if (roundMillis < 6000 || roundMillis > 10000 || confirmedMillis < 0 || confirmedMillis > roundMillis
				|| now == null)
			throw new IllegalArgumentException("采集进度无效");
		if (!foreground)
			return new GatheringRulesProgress(confirmedMillis, null, false);
		if (lastSeen == null)
			return new GatheringRulesProgress(confirmedMillis, now, confirmedMillis == roundMillis);
		long elapsed = Duration.between(lastSeen, now).toMillis();
		if (elapsed < 0)
			return new GatheringRulesProgress(confirmedMillis, lastSeen, confirmedMillis == roundMillis);
		long credit = elapsed > HEARTBEAT_TIMEOUT_MILLIS ? 0 : Math.min(MAX_CREDIT_MILLIS, elapsed);
		long progress = Math.min(roundMillis, confirmedMillis + credit);
		return new GatheringRulesProgress(progress, now, progress == roundMillis);
	}

}
