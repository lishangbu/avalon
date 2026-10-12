package io.github.lishangbu.avalon.world.domain;

/** 固定50毫秒步长；本次按键只影响后续时间，不能回填已过去的操作。 */
public final class FishingRules {

	public static final int TICK_MILLIS = 50;

	public static final int MAX_MILLIS = 20000;

	private FishingRules() {
	}

	public static FishingRulesState start(long now) {
		return new FishingRulesState(now, 0, 0, 0.5, false, "PLAYING");
	}

	/** 断线超过2.5秒或切后台结束本局，不能离线累积钓获。 */
	public static FishingRulesState advance(FishingRulesState state, long now, boolean held, boolean foreground,
			long seed, int difficulty, int skillLevel, int rodTier) {
		validate(difficulty, skillLevel, rodTier);
		if (!state.status().equals("PLAYING"))
			return state;
		if (now < state.lastTickMillis())
			return state;
		long delta = now - state.lastTickMillis();
		if (!foreground || delta > 2500)
			return new FishingRulesState(now, state.elapsedMillis(), state.caughtMillis(), state.position(), false,
					"ESCAPED");
		int elapsed = state.elapsedMillis(), caught = state.caughtMillis();
		double position = state.position();
		long tick = state.lastTickMillis();
		double tolerance = tolerance(difficulty, skillLevel, rodTier);
		String status = "PLAYING";
		while (now - tick >= TICK_MILLIS && elapsed < MAX_MILLIS) {
			elapsed += TICK_MILLIS;
			tick += TICK_MILLIS;
			// 每秒移动30%轨道，配合较慢鱼区摆动，为移动端观察和松手保留反应窗口。
			position = Math.clamp(position + (state.held() ? 0.015 : -0.015), 0, 1);
			double fish = fishPosition(seed, difficulty, elapsed);
			caught = Math.clamp(caught + (Math.abs(position - fish) <= tolerance ? TICK_MILLIS : -TICK_MILLIS / 2), 0,
					8000);
			if (elapsed >= 10000 && caught >= 8000) {
				status = "CAUGHT";
				break;
			}
		}
		if (elapsed >= MAX_MILLIS && !status.equals("CAUGHT"))
			status = "ESCAPED";
		return new FishingRulesState(tick, elapsed, caught, position, held, status);
	}

	public static double tolerance(int difficulty, int skillLevel, int rodTier) {
		validate(difficulty, skillLevel, rodTier);
		return Math.clamp(0.19 - difficulty * 0.008 + (skillLevel - 1) * 0.005 + rodTier * 0.012, 0.1, 0.28);
	}

	public static double fishPosition(long seed, int difficulty, int elapsedMillis) {
		if (difficulty < 1 || difficulty > 10 || elapsedMillis < 0 || elapsedMillis > MAX_MILLIS)
			throw new IllegalArgumentException("鱼种难度或时间无效");
		double phase = Math.floorMod(seed, 6283) / 1000.0;
		double time = elapsedMillis / 1000.0;
		return 0.5 + 0.27 * Math.sin(phase + time * (0.4 + difficulty * 0.025))
				+ 0.08 * Math.sin(phase * 1.7 + time * 1.2);
	}

	private static void validate(int difficulty, int skillLevel, int rodTier) {
		if (difficulty < 1 || difficulty > 10 || skillLevel < 1 || skillLevel > 10 || rodTier < 0 || rodTier > 4)
			throw new IllegalArgumentException("钓鱼参数无效");
	}

}
