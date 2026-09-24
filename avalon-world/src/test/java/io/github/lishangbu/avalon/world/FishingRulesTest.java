package io.github.lishangbu.avalon.world;

import io.github.lishangbu.avalon.world.domain.FishingRules;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FishingRulesTest {

	@Test
	void slowerTrackingRemainsPlayableButConstantInputCannotCatchEvenWithBestBonuses() {
		for (int difficulty = 1; difficulty <= 10; difficulty++) {
			int caught = 0;
			for (int sample = 0; sample < 256; sample++) {
				long seed = sample * 7919L + 31;
				var tracking = FishingRules.start(0);
				for (int time = 0; time <= 20000 && tracking.status().equals("PLAYING"); time += 500) {
					boolean held = tracking.position() < FishingRules.fishPosition(seed, difficulty,
							tracking.elapsedMillis());
					tracking = FishingRules.advance(tracking, time, held, true, seed, difficulty, difficulty,
							(difficulty - 1) / 2);
				}
				if (tracking.status().equals("CAUGHT"))
					caught++;
				for (boolean held : new boolean[] { false, true }) {
					var idle = FishingRules.advance(FishingRules.start(0), 0, held, true, seed, difficulty, 10, 4);
					for (int time = 500; time <= 20000; time += 500)
						idle = FishingRules.advance(idle, time, held, true, seed, difficulty, 10, 4);
					assertEquals("ESCAPED", idle.status(), "固定输入不应自动钓获：难度" + difficulty + " 种子" + seed + " 按住" + held);
				}
			}
			assertTrue(caught >= 244, "难度" + difficulty + "慢响应成功样本不足：" + caught);
		}
	}

	@Test
	void newInputCannotRewritePastMovementOrSpeedUpServerTime() {
		var start = FishingRules.start(1000);
		var pressed = FishingRules.advance(start, 1200, true, true, 42, 1, 1, 0);
		assertEquals(0.44, pressed.position(), 0.000001);
		assertEquals(200, pressed.elapsedMillis());
		assertEquals(pressed, FishingRules.advance(pressed, 1199, false, true, 42, 1, 1, 0));
		assertEquals(pressed, FishingRules.advance(pressed, 1200, true, true, 42, 1, 1, 0));
		var released = FishingRules.advance(pressed, 1400, false, true, 42, 1, 1, 0);
		assertEquals(0.5, released.position(), 0.000001);
	}

	@Test
	void disconnectAndBackgroundEscapeWithoutOfflineCredit() {
		var start = FishingRules.start(1000);
		var lost = FishingRules.advance(start, 5000, true, true, 42, 1, 1, 0);
		assertEquals("ESCAPED", lost.status());
		assertEquals(0, lost.elapsedMillis());
		assertEquals(lost, FishingRules.advance(lost, 6000, true, true, 42, 1, 1, 0));
		assertEquals("ESCAPED", FishingRules.advance(start, 1100, true, false, 42, 1, 1, 0).status());
	}

	@Test
	void trackingCanCatchEveryDifficultyWhileIdleCannotCatch() {
		for (int difficulty = 1; difficulty <= 10; difficulty++) {
			for (long seed : new long[] { 1, 42, 999, -128, Long.MAX_VALUE }) {
				var state = FishingRules.start(0);
				for (int time = 0; time <= 20000 && state.status().equals("PLAYING"); time += 100) {
					boolean held = state.position() < FishingRules.fishPosition(seed, difficulty,
							state.elapsedMillis());
					state = FishingRules.advance(state, time, held, true, seed, difficulty, difficulty,
							(difficulty - 1) / 2);
				}
				assertEquals("CAUGHT", state.status(), "难度" + difficulty + "种子" + seed);
				assertTrue(state.elapsedMillis() >= 10000 && state.elapsedMillis() <= 20000);
				var idle = FishingRules.start(0);
				for (int time = 200; time <= 20000; time += 200)
					idle = FishingRules.advance(idle, time, false, true, seed, difficulty, difficulty,
							(difficulty - 1) / 2);
				assertEquals("ESCAPED", idle.status());
			}
		}
	}

}
