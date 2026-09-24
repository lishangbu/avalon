package io.github.lishangbu.avalon.world.domain;

import java.time.Instant;

/** 浇水前无限暂停，浇水一次启动现实计时；成熟后不枯萎，不依赖后台任务。 */
public final class FarmingRules {

	private FarmingRules() {
	}

	public static Instant water(Instant wateredAt, Instant now) {
		if (now == null)
			throw new IllegalArgumentException("缺少服务端时间");
		return wateredAt == null ? now : wateredAt;
	}

	public static long remainingSeconds(Instant wateredAt, int growthSeconds, Instant now) {
		if (growthSeconds <= 0 || now == null)
			throw new IllegalArgumentException("生长参数无效");
		if (wateredAt == null)
			return growthSeconds;
		long millis = wateredAt.plusSeconds(growthSeconds).toEpochMilli() - now.toEpochMilli();
		return Math.min(growthSeconds, Math.max(0, (millis + 999) / 1000));
	}

	public static boolean mature(Instant wateredAt, int growthSeconds, Instant now) {
		return wateredAt != null && remainingSeconds(wateredAt, growthSeconds, now) == 0;
	}

}
